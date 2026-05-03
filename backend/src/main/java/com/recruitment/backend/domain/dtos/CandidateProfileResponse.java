package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CandidateProfileResponse {
    private UUID candidateId;
    private String fullName;
    private String headline;
    private String phoneNumber;
    private String profilePictureUrl;
    private Boolean openToWork;
    private List<String> skills;
}
