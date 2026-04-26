package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.RegisterRecruiterRequest;
import com.recruitment.backend.domain.dtos.RegisterRecruiterResponse;
import com.recruitment.backend.services.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruiters")
@RequiredArgsConstructor
public class RecruiterController {
    private final RecruiterService recruiterService;

    @PostMapping("/profile")
    public ApiResponse<RegisterRecruiterResponse> createRecruiterProfile(@RequestBody RegisterRecruiterRequest request){
        return ApiResponse.<RegisterRecruiterResponse>builder()
                .result(recruiterService.createRecruiterProfile(request))
                .build();
    }
}
