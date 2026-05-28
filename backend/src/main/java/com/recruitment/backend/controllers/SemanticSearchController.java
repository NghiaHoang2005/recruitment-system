package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.Cv.CvSemanticSearchResponse;
import com.recruitment.backend.domain.dtos.JobSemanticSearchResponse;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import com.recruitment.backend.domain.enums.JobEmbeddingType;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.services.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search/semantic")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobSemanticSearchResponse>>> searchJobs(
            @RequestParam String q,
            @RequestParam(defaultValue = "FULL_JOB") JobEmbeddingType type,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<JobSemanticSearchResponse> response = semanticSearchService.searchJobs(q, type, status, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cvs")
    public ResponseEntity<ApiResponse<List<CvSemanticSearchResponse>>> searchCvs(
            @RequestParam String q,
            @RequestParam(defaultValue = "SUMMARY") EmbeddingType type,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<CvSemanticSearchResponse> response = semanticSearchService.searchCvs(q, type, candidateId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
