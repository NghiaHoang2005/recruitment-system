package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponse {
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
    private String createdById;
    private CompanyStatus status;
}
