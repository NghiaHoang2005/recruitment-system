package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
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
}
