package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminSettingsRequest;
import com.recruitment.backend.domain.dtos.AdminSettingsResponse;
import com.recruitment.backend.domain.entities.AdminSetting;
import com.recruitment.backend.repositories.AdminSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {
    public static final String MODE_MANUAL = "MANUAL";
    public static final String MODE_AUTO_APPROVE = "AUTO_APPROVE";

    private static final String AUTO_APPROVE_VERIFIED_JOBS = "autoApproveJobsFromVerifiedCompanies";
    private static final String REQUIRE_ALL_JOB_APPROVAL = "requireAdminApprovalForAllJobs";
    private static final String COMPANY_VERIFICATION_MODE = "companyVerificationMode";
    private static final String AI_MATCHING_ENABLED = "aiMatchingEnabled";
    private static final String NOTIFY_ADMINS_COMPANY_REVIEW = "notifyAdminsForCompanyReview";
    private static final String NOTIFY_ADMINS_JOB_REVIEW = "notifyAdminsForJobReview";
    private static final String NOTIFY_RECRUITERS_MODERATION = "notifyRecruitersForModeration";
    private static final String NOTIFY_COMPANY_OWNERS_MODERATION = "notifyCompanyOwnersForModeration";

    private final AdminSettingRepository adminSettingRepository;
    private final AdminAuditLogService adminAuditLogService;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminSettingsResponse getSettings() {
        return buildResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminSettingsResponse updateSettings(AdminSettingsRequest request) {
        if (request.getAutoApproveJobsFromVerifiedCompanies() != null) {
            put(AUTO_APPROVE_VERIFIED_JOBS, request.getAutoApproveJobsFromVerifiedCompanies());
        }
        if (request.getRequireAdminApprovalForAllJobs() != null) {
            put(REQUIRE_ALL_JOB_APPROVAL, request.getRequireAdminApprovalForAllJobs());
        }
        if (request.getCompanyVerificationMode() != null) {
            put(COMPANY_VERIFICATION_MODE, normalizeCompanyVerificationMode(request.getCompanyVerificationMode()));
        }
        if (request.getAiMatchingEnabled() != null) {
            put(AI_MATCHING_ENABLED, request.getAiMatchingEnabled());
        }
        if (request.getNotifyAdminsForCompanyReview() != null) {
            put(NOTIFY_ADMINS_COMPANY_REVIEW, request.getNotifyAdminsForCompanyReview());
        }
        if (request.getNotifyAdminsForJobReview() != null) {
            put(NOTIFY_ADMINS_JOB_REVIEW, request.getNotifyAdminsForJobReview());
        }
        if (request.getNotifyRecruitersForModeration() != null) {
            put(NOTIFY_RECRUITERS_MODERATION, request.getNotifyRecruitersForModeration());
        }
        if (request.getNotifyCompanyOwnersForModeration() != null) {
            put(NOTIFY_COMPANY_OWNERS_MODERATION, request.getNotifyCompanyOwnersForModeration());
        }
        adminAuditLogService.record("SETTINGS_UPDATED", "SETTINGS", new UUID(0L, 0L), "Admin cập nhật cấu hình hệ thống.");
        return buildResponse();
    }

    @Transactional(readOnly = true)
    public boolean autoApproveJobsFromVerifiedCompanies() {
        return getBoolean(AUTO_APPROVE_VERIFIED_JOBS, true);
    }

    @Transactional(readOnly = true)
    public boolean requireAdminApprovalForAllJobs() {
        return getBoolean(REQUIRE_ALL_JOB_APPROVAL, false);
    }

    @Transactional(readOnly = true)
    public boolean autoApproveCompanies() {
        return MODE_AUTO_APPROVE.equals(getString(COMPANY_VERIFICATION_MODE, MODE_MANUAL));
    }

    @Transactional(readOnly = true)
    public boolean notifyAdminsForCompanyReview() {
        return getBoolean(NOTIFY_ADMINS_COMPANY_REVIEW, true);
    }

    @Transactional(readOnly = true)
    public boolean notifyAdminsForJobReview() {
        return getBoolean(NOTIFY_ADMINS_JOB_REVIEW, true);
    }

    @Transactional(readOnly = true)
    public boolean notifyRecruitersForModeration() {
        return getBoolean(NOTIFY_RECRUITERS_MODERATION, true);
    }

    @Transactional(readOnly = true)
    public boolean notifyCompanyOwnersForModeration() {
        return getBoolean(NOTIFY_COMPANY_OWNERS_MODERATION, true);
    }

    private AdminSettingsResponse buildResponse() {
        return AdminSettingsResponse.builder()
                .autoApproveJobsFromVerifiedCompanies(getBoolean(AUTO_APPROVE_VERIFIED_JOBS, true))
                .requireAdminApprovalForAllJobs(getBoolean(REQUIRE_ALL_JOB_APPROVAL, false))
                .companyVerificationMode(getString(COMPANY_VERIFICATION_MODE, MODE_MANUAL))
                .aiMatchingEnabled(getBoolean(AI_MATCHING_ENABLED, true))
                .notifyAdminsForCompanyReview(getBoolean(NOTIFY_ADMINS_COMPANY_REVIEW, true))
                .notifyAdminsForJobReview(getBoolean(NOTIFY_ADMINS_JOB_REVIEW, true))
                .notifyRecruitersForModeration(getBoolean(NOTIFY_RECRUITERS_MODERATION, true))
                .notifyCompanyOwnersForModeration(getBoolean(NOTIFY_COMPANY_OWNERS_MODERATION, true))
                .build();
    }

    private void put(String key, Boolean value) {
        put(key, String.valueOf(Boolean.TRUE.equals(value)));
    }

    private void put(String key, String value) {
        AdminSetting setting = adminSettingRepository.findById(key)
                .orElseGet(() -> AdminSetting.builder().key(key).build());
        setting.setValue(value);
        adminSettingRepository.save(setting);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    private String getString(String key, String defaultValue) {
        return adminSettingRepository.findById(key)
                .map(AdminSetting::getValue)
                .orElse(defaultValue);
    }

    private String normalizeCompanyVerificationMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_MANUAL;
        }
        String normalized = mode.trim().toUpperCase();
        return Map.of(MODE_MANUAL, MODE_MANUAL, MODE_AUTO_APPROVE, MODE_AUTO_APPROVE)
                .getOrDefault(normalized, MODE_MANUAL);
    }
}
