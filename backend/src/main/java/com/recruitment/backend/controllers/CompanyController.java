package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    public ApiResponse<CompanyResponse> createCompany(@RequestBody CompanyRequest request){
        return ApiResponse.<CompanyResponse>builder()
                .result(companyService.createCompany(request))
                .build();
    }

    @PostMapping("/{companyId}/join")
    public ApiResponse<CompanyMemberResponse> joinCompany(@PathVariable String companyId){
        return ApiResponse.<CompanyMemberResponse>builder()
                .result(companyService.joinCompany(companyId))
                .build();
    }

    @PostMapping("/{companyId}/members/{userId}/approve")
    public ApiResponse<CompanyMemberResponse> approveRequest(@PathVariable String companyId, @PathVariable String userId){
        return ApiResponse.<CompanyMemberResponse>builder()
                .result(companyService.processRequest(companyId, userId, JoinStatus.APPROVED))
                .build();
    }

    @PostMapping("/{companyId}/members/{userId}/reject")
    public ApiResponse<CompanyMemberResponse> rejectRequest(@PathVariable String companyId, @PathVariable String userId){
        return ApiResponse.<CompanyMemberResponse>builder()
                .result(companyService.processRequest(companyId, userId, JoinStatus.REJECTED))
                .build();
    }
}
