package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.enums.JobReportStatus;
import com.recruitment.backend.services.JobReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class JobReportController {
    private final JobReportService jobReportService;

    @PostMapping("/api/candidate/jobs/{jobId}/reports")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApiResponse<JobReportResponse> reportJob(@PathVariable UUID jobId, @RequestBody JobReportRequest request) {
        return ApiResponse.success(jobReportService.createReport(jobId, request));
    }

    @GetMapping("/api/admin/job-reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminPageResponse<JobReportResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) JobReportStatus status) {
        return ApiResponse.success(jobReportService.getReports(page, size, status));
    }

    @PatchMapping("/api/admin/job-reports/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<JobReportResponse> reviewReport(
            @PathVariable UUID reportId, @RequestBody JobReportReviewRequest request) {
        return ApiResponse.success(jobReportService.reviewReport(reportId, request));
    }
}
