package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {
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
}
