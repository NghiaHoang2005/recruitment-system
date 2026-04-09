package com.recruitment.backend.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.Cv.CvParsedResponse;
import com.recruitment.backend.domain.dtos.Cv.CvUploadCompleteRequest;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.entities.Candidate;
import com.recruitment.backend.domain.entities.Cv;
import com.recruitment.backend.domain.entities.CvStatus;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.AsyncCvProcessor;
import com.recruitment.backend.services.CvService;
import com.recruitment.backend.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<?> processUploadedCv(@RequestBody CvUploadCompleteRequest request) {
        try {
            Cv newCv = cvService.processAndSaveUploadedCv(getCurrentUserId(), request);
            return ResponseEntity.ok(newCv);
        } catch (ResponseStatusException e) {
            // Nắm bắt lỗi từ Service và trả về đúng HTTP Status
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
    @GetMapping("/{cvId}/extracted-data")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<?> getExtractedData(@PathVariable UUID cvId) {
        try {
            Map<String, Object> response = cvService.getExtractedData(getCurrentUserId(), cvId);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/sync-profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<?> syncToProfile(@RequestBody ProfileCandidateUpdateRequest request) {
        try {
            profileService.confirmAndUpdateProfile(getCurrentUserId(), request);
            return ResponseEntity.ok("Hồ sơ của bạn đã được cập nhật thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi xảy ra khi cập nhật hồ sơ: " + e.getMessage());
        }
    }
}
