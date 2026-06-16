package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobReport;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.JobReportStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.JobReportRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobReportService {
    private final JobReportRepository jobReportRepository;
    private final JobRepository jobRepository;
    private final SecurityUtil securityUtil;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public JobReportResponse createReport(UUID jobId, JobReportRequest request) {
        User reporter = securityUtil.getCurrentUser();
        if (request == null || request.getReason() == null) {
            throw new AppException(ErrorCode.JOB_REPORT_REASON_REQUIRED);
        }
        if (jobReportRepository.existsByReporter_IdAndJob_Id(reporter.getId(), jobId)) {
            throw new AppException(ErrorCode.JOB_REPORT_ALREADY_EXISTS);
        }
        Job job = jobRepository.findById(jobId)
                .filter(item -> item.getStatus() == JobStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        return toResponse(jobReportRepository.save(JobReport.builder()
                .job(job)
                .reporter(reporter)
                .reason(request.getReason())
                .details(normalize(request.getDetails()))
                .build()));
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<JobReportResponse> getReports(int page, int size, JobReportStatus status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobReport> reports = status == null
                ? jobReportRepository.findAll(pageable)
                : jobReportRepository.findByStatus(status, pageable);
        return AdminPageResponse.<JobReportResponse>builder()
                .items(reports.stream().map(this::toResponse).toList())
                .page(reports.getNumber()).size(reports.getSize())
                .totalItems(reports.getTotalElements()).totalPages(reports.getTotalPages())
                .build();
    }

    @Transactional
    public JobReportResponse reviewReport(UUID reportId, JobReportReviewRequest request) {
        if (request == null || (request.getStatus() != JobReportStatus.RESOLVED
                && request.getStatus() != JobReportStatus.DISMISSED)) {
            throw new AppException(ErrorCode.JOB_REPORT_INVALID_STATUS);
        }
        JobReport report = jobReportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_REPORT_NOT_FOUND));
        User admin = securityUtil.getCurrentUser();
        report.setStatus(request.getStatus());
        report.setAdminNote(normalize(request.getAdminNote()));
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
        JobReport saved = jobReportRepository.save(report);
        adminAuditLogService.record("REVIEW_JOB_REPORT", "JOB_REPORT", reportId,
                request.getStatus() + (saved.getAdminNote() == null ? "" : ": " + saved.getAdminNote()));
        return toResponse(saved);
    }

    private JobReportResponse toResponse(JobReport report) {
        return JobReportResponse.builder()
                .id(report.getId())
                .jobId(report.getJob().getId())
                .jobTitle(report.getJob().getTitle())
                .companyName(report.getJob().getCompany() == null ? null : report.getJob().getCompany().getName())
                .jobStatus(report.getJob().getStatus().name())
                .reporterId(report.getReporter().getId())
                .reporterEmail(report.getReporter().getEmail())
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .reviewedByEmail(report.getReviewedBy() == null ? null : report.getReviewedBy().getEmail())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
