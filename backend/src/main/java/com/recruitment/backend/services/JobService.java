package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobSummaryDTO;
import com.recruitment.backend.domain.dtos.JobCategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.recruitment.backend.services.ai.pipeline.JobEmbeddingTextBuilder;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collection;
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
    private final JobEmbeddingTextBuilder jobEmbeddingTextBuilder;
    private final JobAsyncProcessingService jobAsyncProcessingService;
    private final JobMapper jobMapper;
    private final NotificationFacade notificationFacade;
    private final AdminSettingsService adminSettingsService;
    private final JobCategoryRepository jobCategoryRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public JobDTO createJob(JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Company company = getApprovedCompanyForRecruiter(recruiter);

        if (company.getStatus() == CompanyStatus.REJECTED || company.getStatus() == CompanyStatus.BLOCKED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        JobStatus status = company.getStatus() == CompanyStatus.ACTIVE
                && adminSettingsService.autoApproveJobsFromVerifiedCompanies()
                && !adminSettingsService.requireAdminApprovalForAllJobs()
                ? JobStatus.PUBLISHED
                : JobStatus.PENDING;

        Job job = Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .workingTime(dto.getWorkingTime())
                .location(dto.getLocation())
                .standardLocation(resolveLocation(dto.getLocationCode()))
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
        replaceCategories(job, dto.getCategories());
        updateNormalizedText(job);

        Job savedJob = jobRepository.save(job);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobAsyncProcessingService.processJobAsync(savedJob.getId());
            }
        });
        if (savedJob.getStatus() == JobStatus.PENDING && adminSettingsService.notifyAdminsForJobReview()) {
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
        job.setStandardLocation(resolveLocation(dto.getLocationCode()));
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
        replaceCategories(job, dto.getCategories());
        updateNormalizedText(job);

        Job savedJob = jobRepository.save(job);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobAsyncProcessingService.processJobAsync(savedJob.getId());
            }
        });
        return jobMapper.toDto(savedJob);
    }

    public Page<JobSummaryDTO> getJobsForUser(String userEmail, Collection<String> authorities, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Pageable sortedPageable = org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        if (hasAuthority(authorities, "ROLE_ADMIN") || hasAuthority(authorities, "ADMIN")) {
            return jobRepository.findAll(sortedPageable).map(jobMapper::toSummaryDto);
        }
        if (hasAuthority(authorities, "ROLE_RECRUITER") || hasAuthority(authorities, "RECRUITER")) {
            return companyMemberRepository.findFirstByUser_IdAndJoinStatus(user.getId(), JoinStatus.APPROVED)
                    .map(membership -> jobRepository.findByCompany_Id(membership.getCompany().getId(), sortedPageable))
                    .orElseGet(() -> jobRepository.findByRecruiterId(user.getId(), sortedPageable))
                    .map(jobMapper::toSummaryDto);
        }
        return jobRepository.findByStatus(JobStatus.PUBLISHED, sortedPageable)
                .map(jobMapper::toSummaryDto);
    }

    public JobDTO getJobById(UUID id, String userEmail, Collection<String> authorities) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        if (job.getStatus() != JobStatus.PUBLISHED && !canViewPrivateJob(job, userEmail, authorities)) {
            throw new AppException(ErrorCode.JOB_NOT_FOUND);
        }
        return jobMapper.toDto(job);
    }

    public List<JobDTO> getJobsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jobRepository.findAllById(ids).stream()
                .filter(job -> job.getStatus() == JobStatus.PUBLISHED)
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    private boolean canViewPrivateJob(Job job, String userEmail, Collection<String> authorities) {
        if (hasAuthority(authorities, "ROLE_ADMIN") || hasAuthority(authorities, "ADMIN")) {
            return true;
        }
        if (!hasAuthority(authorities, "ROLE_RECRUITER") && !hasAuthority(authorities, "RECRUITER")) {
            return false;
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (job.getRecruiter() != null && user.getId().equals(job.getRecruiter().getId())) {
            return true;
        }
        return job.getCompany() != null
                && companyMemberRepository.existsByCompany_IdAndUser_IdAndJoinStatus(
                job.getCompany().getId(),
                user.getId(),
                JoinStatus.APPROVED
        );
    }

    private boolean hasAuthority(Collection<String> authorities, String authority) {
        return authorities != null && authorities.contains(authority);
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

    private void updateNormalizedText(Job job) {
        String combinedText = jobEmbeddingTextBuilder.buildEmbeddingText(job);
        String normalized = textNormalizationService.normalize(combinedText);
        job.setNormalizedText(normalized.isBlank() ? null : normalized);
    }

    private void replaceCategories(Job job, List<JobCategoryDTO> categoryDtos) {
        if (categoryDtos == null || categoryDtos.isEmpty()) {
            throw new AppException(ErrorCode.JOB_CATEGORY_REQUIRED);
        }
        List<String> codes = categoryDtos.stream()
                .map(JobCategoryDTO::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        List<JobCategory> categories = jobCategoryRepository.findByCodeIn(codes);
        if (categories.size() != codes.size()) {
            throw new AppException(ErrorCode.JOB_CATEGORY_INVALID);
        }
        job.getCategories().clear();
        job.getCategories().addAll(categories);
    }

    private Location resolveLocation(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            return null;
        }
        return locationRepository.findByCode(locationCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_INVALID));
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
