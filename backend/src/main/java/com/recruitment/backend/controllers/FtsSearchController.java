package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.Cv.CvFtsSearchResponse;
import com.recruitment.backend.domain.dtos.JobFtsSearchResponse;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.services.FtsSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search/fts")
@RequiredArgsConstructor
public class FtsSearchController {

    private final FtsSearchService ftsSearchService;

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobFtsSearchResponse>>> searchJobs(
            @RequestParam String q,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<JobFtsSearchResponse> response = ftsSearchService.searchJobs(q, status, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cvs")
    public ResponseEntity<ApiResponse<List<CvFtsSearchResponse>>> searchCvs(
            @RequestParam String q,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<CvFtsSearchResponse> response = ftsSearchService.searchCvs(q, candidateId, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
