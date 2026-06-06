package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.repositories.JobRepository;
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
    private final JobEmbeddingPipelineService jobEmbeddingPipelineService;
    private final JobStructuredExtractionService jobStructuredExtractionService;
    private final JobSkillExtractionService jobSkillExtractionService;
    private final TextNormalizationService textNormalizationService;
    private final JobEmbeddingTextBuilder jobEmbeddingTextBuilder;

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
            if (job.getCompany() != null && job.getCompany().getStatus() == com.recruitment.backend.domain.enums.CompanyStatus.ACTIVE) {
                job.setStatus(com.recruitment.backend.domain.enums.JobStatus.PUBLISHED);
                job.setPublishedAt(java.time.LocalDateTime.now());
                jobRepository.save(job);
            }
        } catch (Exception ex) {
            log.warn("Could not generate job embeddings for job {}: {}", job.getId(), ex.getMessage());
        }
    }
}
