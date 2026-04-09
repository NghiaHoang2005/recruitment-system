package com.recruitment.backend.controllers;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.recruitment.backend.domain.dtos.Cv.CvParsedResponse;
import com.recruitment.backend.domain.dtos.Cv.CvUploadCompleteRequest;
import com.recruitment.backend.domain.dtos.ProfileCandidateUpdateRequest;
import com.recruitment.backend.domain.entities.Cv;
import com.recruitment.backend.domain.entities.CvStatus;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.services.AsyncCvProcessor;
import com.recruitment.backend.services.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {
    private final Cloudinary cloudinary;
    private final AsyncCvProcessor asyncCvProcessor;
    private final CvRepository cvRepository;
    private final ProfileService profileService;
    @PostMapping("/process-uploaded")
    public ResponseEntity<?> processUploadedCv(@RequestBody CvUploadCompleteRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        try {
            Map resourceData = cloudinary.api().resource(request.getPublicId(), ObjectUtils.emptyMap());

            Map contextData = (Map) resourceData.get("context");
            Map customData = (Map) contextData.get("custom");
            String uploaderId = (String) customData.get("uploader_id");

            if (uploaderId == null || !uploaderId.equals(String.valueOf(currentUserId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Hành vi gian lận bị từ chối!");
            }

            Cv newCv = new Cv();
            newCv.setCandidateId(currentUserId);
            newCv.setFileUrl(request.getPublicId());
            newCv.setCvName(request.getCvName());
            newCv.setAiStatus(CvStatus.PENDING);

            newCv = cvRepository.save(newCv);
            asyncCvProcessor.processCvInBackground(newCv.getId(), request.getPublicId());
            return ResponseEntity.ok(newCv);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File không tồn tại trên hệ thống!");
        }
    }
    @GetMapping("/{cvId}/extracted-data")
    public ResponseEntity<?> getExtractedData(@PathVariable Long cvId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV không tồn tại"));

        if (!cv.getCandidateId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền truy cập CV này");
        }

        if ("PENDING".equals(cv.getAiStatus())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("AI is still processing");
        } else if ("FAILED".equals(cv.getAiStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi trích xuất CV");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("cvId", cv.getId());
        response.put("parsedData", cv.getParsedData());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/sync-profile")
    public ResponseEntity<?> syncToProfile(@RequestBody ProfileCandidateUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long currentUserId = currentUser.getId();

        try {
            profileService.confirmAndUpdateProfile(currentUserId, request);

            return ResponseEntity.ok("Hồ sơ của bạn đã được cập nhật thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi xảy ra khi cập nhật hồ sơ: " + e.getMessage());
        }
    }
}
