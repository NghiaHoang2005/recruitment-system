package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.AddCustomSectionRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.CreateDraftFromTemplateRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderDraftPageResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderDraftResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderTemplateResponse;
import com.recruitment.backend.domain.dtos.CvBuilder.ReorderCvBuilderSectionsRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.UpdateDraftTemplateRequest;
import com.recruitment.backend.domain.dtos.CvBuilder.UpdateCvBuilderDraftRequest;
import com.recruitment.backend.services.CvBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv-builder")
@RequiredArgsConstructor
public class CvBuilderController {

    private final CvBuilderService cvBuilderService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getClaim("user_id"));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<CvBuilderTemplateResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(cvBuilderService.getActiveTemplates()));
    }

    @PostMapping("/drafts/from-template")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> createDraftFromTemplate(
            @RequestBody CreateDraftFromTemplateRequest request
    ) {
        CvBuilderDraftResponse response = cvBuilderService.createDraftFromTemplate(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> getDraft(@PathVariable UUID draftId) {
        CvBuilderDraftResponse response = cvBuilderService.getDraft(getCurrentUserId(), draftId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<CvBuilderDraftPageResponse>> getDrafts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        CvBuilderDraftPageResponse response = cvBuilderService.getDrafts(getCurrentUserId(), cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> updateDraft(
            @PathVariable UUID draftId,
            @RequestBody UpdateCvBuilderDraftRequest request
    ) {
        CvBuilderDraftResponse response = cvBuilderService.updateDraft(getCurrentUserId(), draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/drafts/{draftId}/template")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> updateDraftTemplate(
            @PathVariable UUID draftId,
            @RequestBody UpdateDraftTemplateRequest request
    ) {
        CvBuilderDraftResponse response = cvBuilderService.updateDraftTemplate(getCurrentUserId(), draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/drafts/{draftId}/sections")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> addCustomSection(
            @PathVariable UUID draftId,
            @RequestBody AddCustomSectionRequest request
    ) {
        CvBuilderDraftResponse response = cvBuilderService.addCustomSection(getCurrentUserId(), draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/drafts/{draftId}/sections/reorder")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> reorderSections(
            @PathVariable UUID draftId,
            @RequestBody ReorderCvBuilderSectionsRequest request
    ) {
        CvBuilderDraftResponse response = cvBuilderService.reorderSections(getCurrentUserId(), draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/drafts/{draftId}/sections/{sectionId}")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> deleteSection(
            @PathVariable UUID draftId,
            @PathVariable String sectionId
    ) {
        CvBuilderDraftResponse response = cvBuilderService.deleteSection(getCurrentUserId(), draftId, sectionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/drafts/{draftId}/publish")
    public ResponseEntity<ApiResponse<CvBuilderDraftResponse>> publishDraft(@PathVariable UUID draftId) {
        CvBuilderDraftResponse response = cvBuilderService.publishDraft(getCurrentUserId(), draftId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
