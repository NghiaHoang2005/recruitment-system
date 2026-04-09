package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobDTO>> createJob(@RequestBody JobDTO jobDTO, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(jobService.createJob(jobDTO, email)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobDTO>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.success(jobService.getAllJobs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDTO>> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobById(id)));
    }
}
