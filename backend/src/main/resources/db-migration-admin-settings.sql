-- Migration: Add admin platform settings
-- Date: 2026-06-06
-- Purpose: Persist configurable admin settings for Phase 9

CREATE TABLE IF NOT EXISTS admin_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO admin_settings (setting_key, setting_value, created_at, updated_at)
VALUES
    ('autoApproveJobsFromVerifiedCompanies', 'true', NOW(), NOW()),
    ('requireAdminApprovalForAllJobs', 'false', NOW(), NOW()),
    ('companyVerificationMode', 'MANUAL', NOW(), NOW()),
    ('aiMatchingEnabled', 'true', NOW(), NOW()),
    ('notifyAdminsForCompanyReview', 'true', NOW(), NOW()),
    ('notifyAdminsForJobReview', 'true', NOW(), NOW()),
    ('notifyRecruitersForModeration', 'true', NOW(), NOW()),
    ('notifyCompanyOwnersForModeration', 'true', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;
