package com.recruitment.backend.domain.dtos.Matching;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MatchingWeightProfileResponse {
    private UUID id;
    private UUID companyId;
    private String name;
    private String version;
    private Double semanticWeight;
    private Double ftsWeight;
    private Double skillsWeight;
    private Double requiredSkillWeight;
    private Double preferredSkillWeight;
    private Boolean active;
    private LocalDateTime createdAt;
}
