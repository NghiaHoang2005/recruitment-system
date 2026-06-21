package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.repositories.UserRepository;
import com.recruitment.backend.services.ai.model.JobStructuredExtractionPayload;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingPipelineService;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingTextBuilder;
import com.recruitment.backend.services.ai.pipeline.JobStructuredExtractionService;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobAsyncProcessingService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobEmbeddingPipelineService jobEmbeddingPipelineService;
    private final JobStructuredExtractionService jobStructuredExtractionService;
    private final JobSkillExtractionService jobSkillExtractionService;
    private final TextNormalizationService textNormalizationService;
    private final JobEmbeddingTextBuilder jobEmbeddingTextBuilder;
    private final AdminSettingsService adminSettingsService;
    private final com.recruitment.backend.notifications.services.NotificationFacade notificationFacade;

    @Async("taskExecutor")
    @Transactional
    public void processJobAsync(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job not found for async processing: {}", jobId);
            return;
        }

        try {
            String language = textNormalizationService.detectLanguage(
                    job.getNormalizedText() == null ? job.getDescription() : job.getNormalizedText()
            );
            String requirementsText = jobEmbeddingTextBuilder.buildRequirementsTextForExtraction(job);
            JobStructuredExtractionPayload payload =
                    jobStructuredExtractionService.extract(job, language, requirementsText);
            job.setParsedData(payload.json());
            jobSkillExtractionService.replaceJobSkills(job, payload.result());
            jobRepository.save(job);
        } catch (Exception e) {
            log.error("Error extracting skills for job {}: {}", jobId, e.getMessage());
        }

        try {
            jobEmbeddingPipelineService.embedAndStore(job);
        } catch (Exception ex) {
            log.warn("Could not generate job embeddings for job {}: {}", job.getId(), ex.getMessage());
        }

        com.recruitment.backend.domain.enums.JobStatus targetStatus = 
            job.getCompany() != null && job.getCompany().getStatus() == com.recruitment.backend.domain.enums.CompanyStatus.ACTIVE
            && adminSettingsService.autoApproveJobsFromVerifiedCompanies()
            && !adminSettingsService.requireAdminApprovalForAllJobs()
            ? com.recruitment.backend.domain.enums.JobStatus.PUBLISHED
            : com.recruitment.backend.domain.enums.JobStatus.PENDING;

        job.setStatus(targetStatus);
        if (targetStatus == com.recruitment.backend.domain.enums.JobStatus.PUBLISHED) {
            job.setPublishedAt(java.time.LocalDateTime.now());
        }
        jobRepository.save(job);

        if (targetStatus == com.recruitment.backend.domain.enums.JobStatus.PENDING && adminSettingsService.notifyAdminsForJobReview()) {
            userRepository.findByRole_NameAndEnabledTrue("ADMIN").forEach(admin -> {
                try {
                    notificationFacade.notifyAdminReviewRequested(
                            admin.getEmail(),
                            job.getTitle(),
                            job.getRecruiter().getEmail(),
                            NotificationType.ADMIN_JOB_REVIEW_REQUESTED,
                            "admin-job-review:" + job.getId() + ":" + admin.getId()
                    );
                } catch (RuntimeException exception) {
                    log.warn("Could not enqueue job review notification for admin {}", admin.getId(), exception);
                }
            });
        }
    }
}
