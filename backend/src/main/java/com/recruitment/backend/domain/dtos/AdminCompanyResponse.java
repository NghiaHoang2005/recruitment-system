package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.CompanyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCompanyResponse {
    private UUID id;
    private String name;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String description;
    private String industry;
    private Integer companySize;
    private String taxCode;
    private String businessLicense;
    private CompanyStatus status;
    private UUID createdById;
    private String ownerEmail;
    private String ownerName;
    private long memberCount;
    private long pendingMemberCount;
    private long openJobCount;
}
