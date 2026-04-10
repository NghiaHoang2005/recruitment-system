package com.recruitment.backend.controllers;

import com.cloudinary.Api;
import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.Cv.CvUploadCompleteRequest;
import com.recruitment.backend.domain.dtos.CvResponse;
import com.recruitment.backend.domain.dtos.PresignedUrlResponse;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.services.CvService;
import com.recruitment.backend.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {
    private final ProfileService profileService;
    private final CvService cvService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return currentUser.getId();
    }

    @PostMapping("/process-uploaded")
    public ResponseEntity<ApiResponse<CvResponse>> processUploadedCv(@RequestBody CvUploadCompleteRequest request) {
        Cv newCv = cvService.processAndSaveUploadedCv(getCurrentUserId(), request);
        CvResponse response = CvResponse.builder()
                .id(newCv.getId())
                .fileName(newCv.getCvName())
                .fileUrl(newCv.getFileUrl())
                .uploadedAt(newCv.getUploadedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @GetMapping("/{cvId}/extracted-data")
    public ResponseEntity<?> getExtractedData(@PathVariable UUID cvId) {
        Map<String, Object> response = cvService.getExtractedData(getCurrentUserId(), cvId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/sync-profile")
    public ResponseEntity<ApiResponse<String>> syncToProfile(@RequestBody ProfileCandidateUpdateRequest request) {
        profileService.confirmAndUpdateProfile(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Hồ sơ của bạn đã được cập nhật thành công!"));
    }

    @GetMapping("/{cvId}/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(@PathVariable UUID cvId) {
        String url = cvService.getPresignedUrl(getCurrentUserId(), cvId);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .downloadUrl(url)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
