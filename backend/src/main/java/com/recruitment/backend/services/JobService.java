package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobRequirementItemDTO;
import com.recruitment.backend.domain.dtos.JobRequirementSectionDTO;
import com.recruitment.backend.domain.entities.*;
import com.recruitment.backend.domain.enums.*;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.*;
import com.recruitment.backend.services.ai.model.JobStructuredExtractionPayload;
import com.recruitment.backend.services.ai.pipeline.JobStructuredExtractionService;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingPipelineService;
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingTextBuilder;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyRepository companyRepository;
    private final TextNormalizationService textNormalizationService;
    private final JobStructuredExtractionService jobStructuredExtractionService;
    private final JobSkillExtractionService jobSkillExtractionService;
    private final JobEmbeddingPipelineService jobEmbeddingPipelineService;
    private final JobEmbeddingTextBuilder jobEmbeddingTextBuilder;
    private final JobMapper jobMapper;
    private final NotificationFacade notificationFacade;

    @Transactional
    public JobDTO createJob(JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Company company = getApprovedCompanyForRecruiter(recruiter);

        if (company.getStatus() == CompanyStatus.REJECTED || company.getStatus() == CompanyStatus.BLOCKED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        JobStatus status = company.getStatus() == CompanyStatus.ACTIVE ? JobStatus.PUBLISHED : JobStatus.PENDING;

        Job job = Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .workingTime(dto.getWorkingTime())
                .location(dto.getLocation())
                .employmentType(dto.getEmploymentType())
                .workMode(dto.getWorkMode())
                .level(dto.getLevel())
                .minSalary(dto.getMinSalary())
                .maxSalary(dto.getMaxSalary())
                .currency(dto.getCurrency())
                .salaryNegotiable(Boolean.TRUE.equals(dto.getSalaryNegotiable()))
                .headcount(dto.getHeadcount())
                .deadline(dto.getDeadline())
                .company(company)
                .status(status)
                .publishedAt(status == JobStatus.PUBLISHED ? LocalDateTime.now() : null)
                .recruiter(recruiter)
                .build();

        replaceRequirementSections(job, dto.getRequirementSections());
        updateNormalizedText(job);

        Job savedJob = jobRepository.save(job);
        extractAndStoreJobSkills(savedJob);
        embedJob(savedJob);
        if (savedJob.getStatus() == JobStatus.PENDING) {
            notifyAdminsJobReviewRequested(savedJob, recruiter);
        }
        return jobMapper.toDto(savedJob);
    }

    @Transactional
    public JobDTO updateJob(UUID id, JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Company company = getApprovedCompanyForRecruiter(recruiter);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setWorkingTime(dto.getWorkingTime());
        job.setLocation(dto.getLocation());
        job.setEmploymentType(dto.getEmploymentType());
        job.setWorkMode(dto.getWorkMode());
        job.setLevel(dto.getLevel());
        job.setMinSalary(dto.getMinSalary());
        job.setMaxSalary(dto.getMaxSalary());
        job.setCurrency(dto.getCurrency());
        job.setSalaryNegotiable(Boolean.TRUE.equals(dto.getSalaryNegotiable()));
        job.setHeadcount(dto.getHeadcount());
        job.setDeadline(dto.getDeadline());

        replaceRequirementSections(job, dto.getRequirementSections());
        updateNormalizedText(job);

        Job savedJob = jobRepository.save(job);
        extractAndStoreJobSkills(savedJob);
        embedJob(savedJob);
        return jobMapper.toDto(savedJob);
    }

    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll().stream().map(jobMapper::toDto).collect(Collectors.toList());
    }

    public JobDTO getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        return jobMapper.toDto(job);
    }

    public List<JobDTO> getJobsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jobRepository.findAllById(ids).stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    private Company getApprovedCompanyForRecruiter(User recruiter) {
        CompanyMember membership = companyMemberRepository.findFirstByUser_IdAndJoinStatus(recruiter.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        return companyRepository.findById(membership.getCompany().getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    }

    private void replaceRequirementSections(Job job, List<JobRequirementSectionDTO> sectionDtos) {
        job.getRequirementSections().clear();
        if (sectionDtos == null || sectionDtos.isEmpty()) {
            return;
        }

        List<JobRequirementSectionDTO> sortedSections = sectionDtos.stream()
                .sorted(Comparator.comparing(section -> section.getDisplayOrder() == null ? 0 : section.getDisplayOrder()))
                .toList();

        int sectionIndex = 0;
        for (JobRequirementSectionDTO sectionDto : sortedSections) {
            if (sectionDto.getTitle() == null || sectionDto.getTitle().isBlank()) {
                continue;
            }
            JobRequirementSection section = JobRequirementSection.builder()
                    .job(job)
                    .title(sectionDto.getTitle().trim())
                    .sectionType(sectionDto.getSectionType() == null ? RequirementSectionType.OTHER : sectionDto.getSectionType())
                    .displayOrder(sectionDto.getDisplayOrder() == null ? sectionIndex : sectionDto.getDisplayOrder())
                    .build();

            List<JobRequirementItemDTO> itemDtos = sectionDto.getItems() == null ? List.of() : sectionDto.getItems();
            int itemIndex = 0;
            for (JobRequirementItemDTO itemDto : itemDtos) {
                if (itemDto.getContent() == null || itemDto.getContent().isBlank()) {
                    continue;
                }
                JobRequirementItem item = JobRequirementItem.builder()
                        .section(section)
                        .content(itemDto.getContent().trim())
                        .displayOrder(itemDto.getDisplayOrder() == null ? itemIndex : itemDto.getDisplayOrder())
                        .build();
                section.getItems().add(item);
                itemIndex++;
            }

            job.getRequirementSections().add(section);
            sectionIndex++;
        }
    }

    private void embedJob(Job job) {
        try {
            jobEmbeddingPipelineService.embedAndStore(job);
        } catch (Exception ex) {
            log.warn("Could not generate job embeddings for job {}: {}", job.getId(), ex.getMessage());
        }
    }

    private void updateNormalizedText(Job job) {
        String combinedText = jobEmbeddingTextBuilder.buildEmbeddingText(job);
        String normalized = textNormalizationService.normalize(combinedText);
        job.setNormalizedText(normalized.isBlank() ? null : normalized);
    }

    private void extractAndStoreJobSkills(Job job) {
        String language = textNormalizationService.detectLanguage(
                job.getNormalizedText() == null ? job.getDescription() : job.getNormalizedText()
        );
        String requirementsText = jobEmbeddingTextBuilder.buildRequirementsTextForExtraction(job);
        JobStructuredExtractionPayload payload =
                jobStructuredExtractionService.extract(job, language, requirementsText);
        job.setParsedData(payload.json());
        jobSkillExtractionService.replaceJobSkills(job, payload.result());
        jobRepository.save(job);
    }

    private void notifyAdminsJobReviewRequested(Job job, User requester) {
        userRepository.findByRole_NameAndEnabledTrue("ADMIN").forEach(admin -> {
            try {
                notificationFacade.notifyAdminReviewRequested(
                        admin.getEmail(),
                        job.getTitle(),
                        requester.getEmail(),
                        NotificationType.ADMIN_JOB_REVIEW_REQUESTED,
                        "admin-job-review:" + job.getId() + ":" + admin.getId()
                );
            } catch (RuntimeException exception) {
                log.warn("Could not enqueue job review notification for admin {}", admin.getId(), exception);
            }
        });
    }

}
