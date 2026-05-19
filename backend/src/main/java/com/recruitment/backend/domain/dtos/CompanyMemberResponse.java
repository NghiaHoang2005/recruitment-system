package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.CompanyRole;
import com.recruitment.backend.domain.enums.JoinStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CompanyMemberResponse {
    private String companyId;
    private String companyName;
    private String userId;
    private String fullName;
    private String email;
    private JoinStatus joinStatus;
    private CompanyRole role;
    private String reviewedBy;
    private LocalDate requestedAt;
    private LocalDate reviewedAt;
}
