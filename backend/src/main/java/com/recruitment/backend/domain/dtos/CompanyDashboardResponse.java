package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.CompanyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyDashboardResponse {
    private String companyId;
    private String name;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String description;
    private String industry;
    private int companySize;
    private String taxCode;
    private String businessLicense;
    private String logoUrl;
    private CompanyStatus status;
    private CompanyRole currentUserCompanyRole;
    private long memberCount;
    private long openJobCount;
    private long pipelineCandidateCount;
    private long pendingRequestCount;
}
