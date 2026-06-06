package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsResponse {
    private Boolean autoApproveJobsFromVerifiedCompanies;
    private Boolean requireAdminApprovalForAllJobs;
    private String companyVerificationMode;
    private Boolean aiMatchingEnabled;
    private Boolean notifyAdminsForCompanyReview;
    private Boolean notifyAdminsForJobReview;
    private Boolean notifyRecruitersForModeration;
    private Boolean notifyCompanyOwnersForModeration;
}
