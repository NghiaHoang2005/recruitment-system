package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.services.AdminAnalyticsService;
import com.recruitment.backend.services.AdminAuditLogService;
import com.recruitment.backend.services.AdminCompanyService;
import com.recruitment.backend.services.AdminDashboardService;
import com.recruitment.backend.services.AdminJobService;
import com.recruitment.backend.services.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;
    private final AdminCompanyService adminCompanyService;
    private final AdminJobService adminJobService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminAuditLogService adminAuditLogService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminDashboardService.getDashboard());
    }

    @GetMapping("/users")
    public ApiResponse<AdminPageResponse<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.success(adminUserService.getUsers(page, size, keyword, role, enabled));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(adminUserService.getUser(userId));
    }

    @PatchMapping("/users/{userId}/disable")
    public ApiResponse<AdminUserResponse> disableUser(
            @PathVariable UUID userId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminUserService.disableUser(userId, reasonOf(request)));
    }

    @PatchMapping("/users/{userId}/enable")
    public ApiResponse<AdminUserResponse> enableUser(
            @PathVariable UUID userId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminUserService.enableUser(userId, reasonOf(request)));
    }

    @GetMapping("/companies")
    public ApiResponse<AdminPageResponse<AdminCompanyResponse>> getCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CompanyStatus status
    ) {
        return ApiResponse.success(adminCompanyService.getCompanies(page, size, keyword, status));
    }

    @GetMapping("/companies/{companyId}")
    public ApiResponse<AdminCompanyResponse> getCompany(@PathVariable UUID companyId) {
        return ApiResponse.success(adminCompanyService.getCompany(companyId));
    }

    @PatchMapping("/companies/{companyId}/verify")
    public ApiResponse<AdminCompanyResponse> verifyCompany(
            @PathVariable UUID companyId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminCompanyService.verifyCompany(companyId, reasonOf(request)));
    }

    @PatchMapping("/companies/{companyId}/reject")
    public ApiResponse<AdminCompanyResponse> rejectCompany(
            @PathVariable UUID companyId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminCompanyService.rejectCompany(companyId, reasonOf(request)));
    }

    @PatchMapping("/companies/{companyId}/request-more-info")
    public ApiResponse<AdminCompanyResponse> requestMoreInfo(
            @PathVariable UUID companyId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminCompanyService.requestMoreInfo(companyId, reasonOf(request)));
    }

    @GetMapping("/jobs")
    public ApiResponse<AdminPageResponse<AdminJobResponse>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) CompanyStatus companyStatus,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ApiResponse.success(adminJobService.getJobs(page, size, keyword, status, companyStatus, fromDate, toDate));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<AdminJobResponse> getJob(@PathVariable UUID jobId) {
        return ApiResponse.success(adminJobService.getJob(jobId));
    }

    @PatchMapping("/jobs/{jobId}/approve")
    public ApiResponse<AdminJobResponse> approveJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminJobService.approveJob(jobId, reasonOf(request)));
    }

    @PatchMapping("/jobs/{jobId}/reject")
    public ApiResponse<AdminJobResponse> rejectJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminJobService.rejectJob(jobId, reasonOf(request)));
    }

    @PatchMapping("/jobs/{jobId}/flag")
    public ApiResponse<AdminJobResponse> flagJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminJobService.flagJob(jobId, reasonOf(request)));
    }

    @PatchMapping("/jobs/{jobId}/unflag")
    public ApiResponse<AdminJobResponse> unflagJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminJobService.unflagJob(jobId, reasonOf(request)));
    }

    @PatchMapping("/jobs/{jobId}/close")
    public ApiResponse<AdminJobResponse> closeJob(
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminModerationRequest request
    ) {
        return ApiResponse.success(adminJobService.closeJob(jobId, reasonOf(request)));
    }

    @GetMapping("/analytics/overview")
    public ApiResponse<AdminAnalyticsOverviewResponse> getAnalyticsOverview(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ApiResponse.success(adminAnalyticsService.getOverview(fromDate, toDate));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<AdminPageResponse<AdminAuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID adminUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ApiResponse.success(adminAuditLogService.getAuditLogs(
                page,
                size,
                adminUserId,
                action,
                targetType,
                targetId,
                fromDate,
                toDate
        ));
    }

    private String reasonOf(AdminModerationRequest request) {
        return request != null ? request.getReason() : null;
    }
}
