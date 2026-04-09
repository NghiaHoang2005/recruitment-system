package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileCandidateUpdateRequest {
    private String fullName;
    private String headline;
    private String phoneNumber;

    private List<String> confirmedSkills;
}
