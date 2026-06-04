package com.recruitment.backend.services;

import com.recruitment.backend.config.HybridMatchingProperties;
import com.recruitment.backend.domain.dtos.Matching.MatchingWeightProfileRequest;
import com.recruitment.backend.domain.entities.Matching.MatchingWeightProfile;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CompanyRepository;
import com.recruitment.backend.repositories.MatchingWeightProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingWeightService {

    private final MatchingWeightProfileRepository matchingWeightProfileRepository;
    private final CompanyRepository companyRepository;
    private final HybridMatchingProperties hybridMatchingProperties;

    public MatchingWeights resolveWeightsForCompany(UUID companyId) {
        MatchingWeightProfile profile = null;
        if (companyId != null) {
            profile = matchingWeightProfileRepository
                    .findFirstByCompanyIdAndActiveTrueOrderByUpdatedAtDesc(companyId)
                    .orElse(null);
        }
        if (profile == null) {
            profile = matchingWeightProfileRepository
                    .findFirstByCompanyIdIsNullAndActiveTrueOrderByUpdatedAtDesc()
                    .orElse(null);
        }
        if (profile == null) {
            return MatchingWeights.fromConfig(hybridMatchingProperties);
        }
        return MatchingWeights.fromProfile(profile);
    }

    public MatchingWeights resolveWeightsByProfileId(UUID profileId) {
        MatchingWeightProfile profile = matchingWeightProfileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));
        return MatchingWeights.fromProfile(profile);
    }

    public Optional<MatchingWeightProfile> getProfile(UUID profileId) {
        return matchingWeightProfileRepository.findById(profileId);
    }

    public List<MatchingWeightProfile> getProfilesByCompany(UUID companyId) {
        return matchingWeightProfileRepository.findByCompanyIdOrderByUpdatedAtDesc(companyId);
    }

    public Optional<MatchingWeightProfile> getActiveProfile(UUID companyId) {
        if (companyId == null) {
            return matchingWeightProfileRepository.findFirstByCompanyIdIsNullAndActiveTrueOrderByUpdatedAtDesc();
        }
        return matchingWeightProfileRepository.findFirstByCompanyIdAndActiveTrueOrderByUpdatedAtDesc(companyId);
    }

    @Transactional
    public MatchingWeightProfile createProfile(MatchingWeightProfileRequest request) {
        UUID companyId = request.getCompanyId();
        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        String version = Optional.ofNullable(request.getVersion())
                .filter(value -> !value.isBlank())
                .orElse(hybridMatchingProperties.getVersion());

        double semantic = clampWeight(Optional.ofNullable(request.getSemanticWeight())
                .orElse(hybridMatchingProperties.getWeights().getSemantic()));
        double fts = clampWeight(Optional.ofNullable(request.getFtsWeight())
                .orElse(hybridMatchingProperties.getWeights().getFts()));
        double skills = clampWeight(Optional.ofNullable(request.getSkillsWeight())
                .orElse(hybridMatchingProperties.getWeights().getSkills()));
        double required = clampWeight(Optional.ofNullable(request.getRequiredSkillWeight())
                .orElse(hybridMatchingProperties.getRequiredSkillWeight()));
        double preferred = clampWeight(Optional.ofNullable(request.getPreferredSkillWeight())
                .orElse(hybridMatchingProperties.getPreferredSkillWeight()));

        boolean active = request.getActive() == null || request.getActive();
        if (active) {
            if (companyId == null) {
                matchingWeightProfileRepository.deactivateActiveGlobal();
            } else {
                matchingWeightProfileRepository.deactivateActiveByCompany(companyId);
            }
        }

        MatchingWeightProfile profile = matchingWeightProfileRepository.save(MatchingWeightProfile.builder()
                .companyId(companyId)
                .name(request.getName())
                .version(version)
                .semanticWeight(semantic)
                .ftsWeight(fts)
                .skillsWeight(skills)
                .requiredSkillWeight(required)
                .preferredSkillWeight(preferred)
                .active(active)
                .build());

        return profile;
    }

    @Transactional
    public MatchingWeightProfile updateProfile(UUID profileId, MatchingWeightProfileRequest request) {
        MatchingWeightProfile profile = matchingWeightProfileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));

        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getSemanticWeight() != null) {
            profile.setSemanticWeight(clampWeight(request.getSemanticWeight()));
        }
        if (request.getFtsWeight() != null) {
            profile.setFtsWeight(clampWeight(request.getFtsWeight()));
        }
        if (request.getSkillsWeight() != null) {
            profile.setSkillsWeight(clampWeight(request.getSkillsWeight()));
        }
        if (request.getRequiredSkillWeight() != null) {
            profile.setRequiredSkillWeight(clampWeight(request.getRequiredSkillWeight()));
        }
        if (request.getPreferredSkillWeight() != null) {
            profile.setPreferredSkillWeight(clampWeight(request.getPreferredSkillWeight()));
        }
        if (request.getActive() != null) {
            boolean newActive = request.getActive();
            if (newActive && !profile.getActive()) {
                if (profile.getCompanyId() == null) {
                    matchingWeightProfileRepository.deactivateActiveGlobal();
                } else {
                    matchingWeightProfileRepository.deactivateActiveByCompany(profile.getCompanyId());
                }
            }
            profile.setActive(newActive);
        }

        return matchingWeightProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(UUID profileId) {
        MatchingWeightProfile profile = matchingWeightProfileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));
        matchingWeightProfileRepository.delete(profile);
    }

    private double clampWeight(double value) {
        return Math.max(0.0, value);
    }
}


