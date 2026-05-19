package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.CompanyInviteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CompanyInviteResponse {
    private String id;
    private String companyId;
    private String email;
    private CompanyInviteStatus status;
    private LocalDate sentAt;
    private String invitedBy;
}
