package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping("/api/applications")
    public ApiResponse<ApplicationResponse> apply(@RequestBody ApplicationRequest request) {
        return ApiResponse.success(applicationService.apply(request));
    }

    @GetMapping("/api/applications/me")
    public ApiResponse<List<ApplicationResponse>> getMyApplications() {
        return ApiResponse.success(applicationService.getMyApplications());
    }

    @GetMapping("/api/applications/me/{applicationId}")
    public ApiResponse<ApplicationResponse> getMyApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(applicationService.getMyApplication(applicationId));
    }

    @PatchMapping("/api/applications/{applicationId}/withdraw")
    public ApiResponse<ApplicationResponse> withdraw(@PathVariable UUID applicationId) {
        return ApiResponse.success(applicationService.withdraw(applicationId));
    }

    @GetMapping("/api/recruiter/applications")
    public ApiResponse<List<ApplicationResponse>> getRecruiterApplications() {
        return ApiResponse.success(applicationService.getRecruiterApplications());
    }

    @GetMapping("/api/recruiter/jobs/{jobId}/applications")
    public ApiResponse<List<ApplicationResponse>> getRecruiterJobApplications(@PathVariable UUID jobId) {
        return ApiResponse.success(applicationService.getRecruiterJobApplications(jobId));
    }

    @GetMapping("/api/recruiter/applications/{applicationId}")
    public ApiResponse<ApplicationResponse> getRecruiterApplication(@PathVariable UUID applicationId) {
        return ApiResponse.success(applicationService.getRecruiterApplication(applicationId));
    }

    @PatchMapping("/api/recruiter/applications/{applicationId}/status")
    public ApiResponse<ApplicationResponse> updateStatus(
            @PathVariable UUID applicationId,
            @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return ApiResponse.success(applicationService.updateStatus(applicationId, request.getStatus()));
    }
}
