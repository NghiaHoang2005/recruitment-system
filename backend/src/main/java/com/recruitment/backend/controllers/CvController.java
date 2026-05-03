package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.Cv.*;
import com.recruitment.backend.domain.dtos.CvResponse;
import com.recruitment.backend.domain.dtos.PresignedUrlResponse;
import com.recruitment.backend.services.CvService;
import com.recruitment.backend.services.CvReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv")
@RequiredArgsConstructor
public class CvController {
    private final CvService cvService;
    private final CvReviewService cvReviewService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt =  (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id"));
    }

    @PostMapping("/process-uploaded")
    public ResponseEntity<ApiResponse<CvResponse>> processUploadedCv(@ModelAttribute CvUploadRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cvService.processAndSaveUploadedCv(getCurrentUserId(), request)));
    }
    @GetMapping("/{cvId}/extracted-data")
    public ResponseEntity<?> getExtractedData(@PathVariable UUID cvId) {
        Map<String, Object> response = cvService.getExtractedData(getCurrentUserId(), cvId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{cvId}/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(@PathVariable UUID cvId) {
        String url = cvService.getPresignedUrl(getCurrentUserId(), cvId);

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .downloadUrl(url)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{cvId}/retry")
    public ResponseEntity<ApiResponse<String>> retryCvExtraction(@PathVariable UUID cvId) {
        cvService.retryCvExtraction(getCurrentUserId(), cvId);
        return ResponseEntity.ok(ApiResponse.success("CV được gửi lại xử lý. Vui lòng chờ kết quả."));
    }

    @GetMapping("/{cvId}/extraction-status")
    public ResponseEntity<ApiResponse<ExtractionStatusResponse>> getExtractionStatus(@PathVariable UUID cvId) {
        ExtractionStatusResponse status = cvService.getExtractionStatus(getCurrentUserId(), cvId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping("/{cvId}/review")
    public ResponseEntity<ApiResponse<CvReviewResponse>> reviewCv(
            @PathVariable UUID cvId,
            @RequestBody(required = false) CvReviewRequest request
    ) {
        UUID jobId = request != null ? request.getJobId() : null;
        CvReviewResponse response = cvReviewService.createReview(getCurrentUserId(), cvId, jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{cvId}/review")
    public ResponseEntity<ApiResponse<CvReviewResponse>> getLatestReview(
            @PathVariable UUID cvId,
            @RequestParam(required = false) UUID jobId
    ) {
        CvReviewResponse response = cvReviewService.getLatestReview(getCurrentUserId(), cvId, jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CvItemResponse>>> getCurrentUserCv() {
        List<CvItemResponse> response = cvService.getCurrentUserCv(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{cvId}/default")
    public ResponseEntity<ApiResponse<String>> setDefaultCv(@PathVariable UUID cvId) {
        cvService.setDefaultCv(getCurrentUserId(), cvId);
        return ResponseEntity.ok(ApiResponse.success("CV đã được đặt làm mặc định."));
    }

    @DeleteMapping("/{cvId}")
    public ResponseEntity<ApiResponse<String>> deleteCv(@PathVariable UUID cvId) {
        cvService.deleteCv(getCurrentUserId(), cvId);
        return ResponseEntity.ok(ApiResponse.success("CV đã được xóa thành công."));
    }
}
