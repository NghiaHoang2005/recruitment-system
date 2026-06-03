package com.recruitment.backend.services;

import com.recruitment.backend.config.HybridMatchingProperties;
import com.recruitment.backend.domain.entities.Matching.MatchingWeightProfile;

import java.util.UUID;

public record MatchingWeights(
        double semanticWeight,
        double ftsWeight,
        double skillsWeight,
        double requiredSkillWeight,
        double preferredSkillWeight,
        UUID profileId,
        String version
) {
    public static MatchingWeights fromProfile(MatchingWeightProfile profile) {
        return new MatchingWeights(
                profile.getSemanticWeight(),
                profile.getFtsWeight(),
                profile.getSkillsWeight(),
                profile.getRequiredSkillWeight(),
                profile.getPreferredSkillWeight(),
                profile.getId(),
                profile.getVersion()
        );
    }

    public static MatchingWeights fromConfig(HybridMatchingProperties properties) {
        return new MatchingWeights(
                properties.getWeights().getSemantic(),
                properties.getWeights().getFts(),
                properties.getWeights().getSkills(),
                properties.getRequiredSkillWeight(),
                properties.getPreferredSkillWeight(),
                null,
                properties.getVersion()
        );
    }
}
