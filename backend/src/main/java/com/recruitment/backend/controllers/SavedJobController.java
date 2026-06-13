package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.SavedJobResponse;
import com.recruitment.backend.services.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidate/saved-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class SavedJobController {
    private final SavedJobService savedJobService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedJobResponse>>> getSavedJobs(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(savedJobService.getSavedJobs(getCurrentUserId(authentication))));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countSavedJobs(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(savedJobService.countSavedJobs(getCurrentUserId(authentication))));
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSavedStatus(
            @PathVariable UUID jobId,
            Authentication authentication) {
        boolean saved = savedJobService.isSaved(getCurrentUserId(authentication), jobId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("saved", saved)));
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<ApiResponse<SavedJobResponse>> saveJob(
            @PathVariable UUID jobId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(savedJobService.saveJob(getCurrentUserId(authentication), jobId)));
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> removeSavedJob(
            @PathVariable UUID jobId,
            Authentication authentication) {
        savedJobService.removeSavedJob(getCurrentUserId(authentication), jobId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private UUID getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id"));
    }
}
