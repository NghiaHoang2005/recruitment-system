package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.RecruiterDashboardResponse;
import com.recruitment.backend.services.RecruiterDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecruiterDashboardController {
    private final RecruiterDashboardService recruiterDashboardService;

    @GetMapping("/api/recruiter/dashboard")
    public ApiResponse<RecruiterDashboardResponse> getDashboard() {
        return ApiResponse.success(recruiterDashboardService.getDashboard());
    }
}
