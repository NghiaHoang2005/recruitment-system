package com.recruitment.backend.domain.dtos.Matching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingWeightProfileRequest {
    private UUID companyId;
    private String name;
    private String version;
    private Double semanticWeight;
    private Double ftsWeight;
    private Double skillsWeight;
    private Double requiredSkillWeight;
    private Double preferredSkillWeight;
    private Boolean active;
}
