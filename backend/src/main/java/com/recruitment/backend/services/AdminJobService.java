package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminJobResponse;
import com.recruitment.backend.domain.dtos.AdminPageResponse;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.AdminMapper;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.ApplicationRepository;
import com.recruitment.backend.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminJobService {
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AdminMapper adminMapper;
    private final AdminAuditLogService adminAuditLogService;
    private final NotificationFacade notificationFacade;
    private final AdminSettingsService adminSettingsService;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminJobResponse> getJobs(
            int page,
            int size,
            String keyword,
            JobStatus status,
            CompanyStatus companyStatus,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Job> jobs = jobRepository.searchAdminJobs(
                normalize(keyword),
                status,
                companyStatus,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(LocalTime.MAX) : null,
                pageable
        );

        return AdminPageResponse.<AdminJobResponse>builder()
                .items(jobs.stream().map(this::toJobResponse).toList())
                .page(jobs.getNumber())
                .size(jobs.getSize())
                .totalItems(jobs.getTotalElements())
                .totalPages(jobs.getTotalPages())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminJobResponse getJob(UUID jobId) {
        return toJobResponse(findJob(jobId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminJobResponse approveJob(UUID jobId, String reason) {
        Job job = findJob(jobId);
        job.setStatus(JobStatus.PUBLISHED);
        if (job.getPublishedAt() == null) {
            job.setPublishedAt(LocalDateTime.now());
        }
        job.setClosedAt(null);
        Job savedJob = jobRepository.save(job);
        adminAuditLogService.record("JOB_APPROVED", "JOB", jobId, reason);
        if (adminSettingsService.notifyRecruitersForModeration()) {
            notifyRecruiter(savedJob, NotificationType.JOB_APPROVED, reason, "job-approved:" + jobId);
        }
        return toJobResponse(savedJob);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminJobResponse rejectJob(UUID jobId, String reason) {
        Job job = findJob(jobId);
        job.setStatus(JobStatus.REJECTED);
        job.setClosedAt(null);
        Job savedJob = jobRepository.save(job);
        adminAuditLogService.record("JOB_REJECTED", "JOB", jobId, reason);
        if (adminSettingsService.notifyRecruitersForModeration()) {
            notifyRecruiter(savedJob, NotificationType.JOB_REJECTED, reason, "job-rejected:" + jobId);
        }
        return toJobResponse(savedJob);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminJobResponse flagJob(UUID jobId, String reason) {
        Job job = findJob(jobId);
        job.setStatus(JobStatus.FLAGGED);
        Job savedJob = jobRepository.save(job);
        adminAuditLogService.record("JOB_FLAGGED", "JOB", jobId, reason);
        if (adminSettingsService.notifyRecruitersForModeration()) {
            notifyRecruiter(savedJob, NotificationType.JOB_FLAGGED, reason, "job-flagged:" + jobId);
        }
        return toJobResponse(savedJob);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminJobResponse unflagJob(UUID jobId, String reason) {
        Job job = findJob(jobId);
        job.setStatus(job.getPublishedAt() == null ? JobStatus.PENDING : JobStatus.PUBLISHED);
        Job savedJob = jobRepository.save(job);
        adminAuditLogService.record("JOB_UNFLAGGED", "JOB", jobId, reason);
        return toJobResponse(savedJob);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminJobResponse closeJob(UUID jobId, String reason) {
        Job job = findJob(jobId);
        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);
        adminAuditLogService.record("JOB_CLOSED", "JOB", jobId, reason);
        if (adminSettingsService.notifyRecruitersForModeration()) {
            notifyRecruiter(savedJob, NotificationType.JOB_CLOSED, reason, "job-closed:" + jobId);
        }
        return toJobResponse(savedJob);
    }

    private Job findJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
    }

    private AdminJobResponse toJobResponse(Job job) {
        return adminMapper.toJobResponse(job, applicationRepository.countByJob_Id(job.getId()));
    }

    private void notifyRecruiter(Job job, NotificationType notificationType, String reason, String idempotencyKey) {
        String recruiterEmail = job.getRecruiter() != null ? job.getRecruiter().getEmail() : null;
        if (recruiterEmail == null || recruiterEmail.isBlank()) {
            return;
        }
        try {
            notificationFacade.notifyJobModeration(
                    recruiterEmail,
                    job.getTitle(),
                    job.getCompany() != null ? job.getCompany().getName() : null,
                    notificationType,
                    reason,
                    idempotencyKey
            );
        } catch (RuntimeException exception) {
            log.warn("Could not enqueue job moderation notification for job {}", job.getId(), exception);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
