package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.Matching.MatchingWeightProfileRequest;
import com.recruitment.backend.domain.dtos.Matching.MatchingWeightProfileResponse;
import com.recruitment.backend.domain.entities.Matching.MatchingWeightProfile;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.services.MatchingWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matching/weight-profiles")
@RequiredArgsConstructor
public class MatchingWeightProfileController {

    private final MatchingWeightService matchingWeightService;

    @PostMapping
    public ResponseEntity<MatchingWeightProfileResponse> createProfile(
            @RequestBody MatchingWeightProfileRequest request) {
        MatchingWeightProfile profile = matchingWeightService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(profile));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchingWeightProfileResponse> getProfile(@PathVariable UUID id) {
        MatchingWeightProfile profile = matchingWeightService.getProfile(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));
        return ResponseEntity.ok(toResponse(profile));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<MatchingWeightProfileResponse>> getProfilesByCompany(
            @PathVariable UUID companyId) {
        List<MatchingWeightProfile> profiles = matchingWeightService.getProfilesByCompany(companyId);
        return ResponseEntity.ok(profiles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/active/{companyId}")
    public ResponseEntity<MatchingWeightProfileResponse> getActiveProfile(
            @PathVariable UUID companyId) {
        MatchingWeightProfile profile = matchingWeightService.getActiveProfile(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.MATCHING_WEIGHT_PROFILE_NOT_FOUND));
        return ResponseEntity.ok(toResponse(profile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchingWeightProfileResponse> updateProfile(
            @PathVariable UUID id,
            @RequestBody MatchingWeightProfileRequest request) {
        MatchingWeightProfile profile = matchingWeightService.updateProfile(id, request);
        return ResponseEntity.ok(toResponse(profile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
        matchingWeightService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

    private MatchingWeightProfileResponse toResponse(MatchingWeightProfile profile) {
        return MatchingWeightProfileResponse.builder()
                .id(profile.getId())
                .companyId(profile.getCompanyId())
                .name(profile.getName())
                .semanticWeight(profile.getSemanticWeight())
                .ftsWeight(profile.getFtsWeight())
                .skillsWeight(profile.getSkillsWeight())
                .requiredSkillWeight(profile.getRequiredSkillWeight())
                .preferredSkillWeight(profile.getPreferredSkillWeight())
                .active(profile.getActive())
                .version(profile.getVersion())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
