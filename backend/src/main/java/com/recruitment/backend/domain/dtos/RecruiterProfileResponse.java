package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RecruiterProfileResponse {
    private UUID recruiterId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private String headline;
    private String companyName;
    private String companyRole;
}
