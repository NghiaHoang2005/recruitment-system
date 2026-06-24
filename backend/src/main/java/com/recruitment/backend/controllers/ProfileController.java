package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.CandidateProfileResponse;
import com.recruitment.backend.domain.dtos.OpenToWorkUpdateRequest;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.dtos.RecruiterProfileResponse;
import com.recruitment.backend.domain.dtos.RecruiterProfileUpdateRequest;
import com.recruitment.backend.domain.dtos.RegisterCandidateProfileRequest;
import com.recruitment.backend.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id"));
    }

    // ─── Candidate ────────────────────────────────────────────────────────────

    @GetMapping("/candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getCandidateProfile() {
        CandidateProfileResponse response = profileService.getCandidateProfile(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getPublicCandidateProfile(@PathVariable UUID candidateId) {
        CandidateProfileResponse response = profileService.getPublicCandidateProfile(getCurrentUserId(), candidateId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> createCandidateProfile(
            @RequestBody RegisterCandidateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.createCandidateProfile(getCurrentUserId(), request)));
    }

    @PutMapping("/candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateCandidateProfile(
            @RequestBody ProfileCandidateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.confirmAndUpdateProfile(getCurrentUserId(), request)));
    }

    @PutMapping("/candidate/open-to-work")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateOpenToWork(
            @RequestBody OpenToWorkUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateOpenToWork(getCurrentUserId(), request)));
    }

    @PutMapping(value = "/candidate/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateCandidateAvatar(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateAvatar(getCurrentUserId(), file)));
    }

    // ─── Recruiter ────────────────────────────────────────────────────────────

    @GetMapping("/recruiter")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> getRecruiterProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getRecruiterProfile(getCurrentUserId())));
    }

    @PutMapping("/recruiter")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> updateRecruiterProfile(
            @RequestBody RecruiterProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateRecruiterProfile(getCurrentUserId(), request)));
    }

    @PutMapping(value = "/recruiter/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> updateRecruiterAvatar(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateRecruiterAvatar(getCurrentUserId(), file)));
    }
}
