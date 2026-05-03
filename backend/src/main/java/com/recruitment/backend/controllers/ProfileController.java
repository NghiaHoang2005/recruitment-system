package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.CandidateProfileResponse;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/candidate")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getCandidateProfile() {
        CandidateProfileResponse response = profileService.getCandidateProfile(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/candidate")
    public ResponseEntity<ApiResponse<String>> updateCandidateProfile(@RequestBody ProfileCandidateUpdateRequest request) {
        profileService.confirmAndUpdateProfile(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Hồ sơ của bạn đã được cập nhật thành công!"));
    }
}
