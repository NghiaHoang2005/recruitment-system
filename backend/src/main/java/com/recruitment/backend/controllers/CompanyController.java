package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.enums.JoinStatus;
import com.recruitment.backend.services.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping("/me")
    public ApiResponse<CompanyDashboardResponse> getMyCompany(){
        return ApiResponse.<CompanyDashboardResponse>builder()
                .result(companyService.getMyCompany())
                .build();
    }

    @PutMapping("/{companyId}")
    public ApiResponse<CompanyDashboardResponse> updateCompany(@PathVariable String companyId, @RequestBody CompanyRequest request){
        return ApiResponse.<CompanyDashboardResponse>builder()
                .result(companyService.updateCompany(companyId, request))
                .build();
    }

    @PutMapping(value = "/{companyId}/logo", consumes = "multipart/form-data")
    public ApiResponse<CompanyDashboardResponse> updateCompanyLogo(@PathVariable String companyId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<CompanyDashboardResponse>builder()
                .result(companyService.updateCompanyLogo(companyId, file))
                .build();
    }

    @GetMapping("/my-memberships")
    public ApiResponse<List<CompanyMemberResponse>> getMyMemberships(){
        return ApiResponse.<List<CompanyMemberResponse>>builder()
                .result(companyService.getMyApprovedMemberships())
                .build();
    }

    @GetMapping("/{companyId}/members/pending")
    public ApiResponse<List<CompanyMemberResponse>> getPendingRequests(@PathVariable String companyId){
        return ApiResponse.<List<CompanyMemberResponse>>builder()
                .result(companyService.getPendingRequests(companyId))
                .build();
    }

    @GetMapping("/{companyId}/members")
    public ApiResponse<List<CompanyMemberResponse>> getMembers(@PathVariable String companyId){
        return ApiResponse.<List<CompanyMemberResponse>>builder()
                .result(companyService.getCompanyMembers(companyId))
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

    @DeleteMapping("/{companyId}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable String companyId, @PathVariable String userId){
        companyService.removeMember(companyId, userId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{companyId}/invites")
    public ApiResponse<CompanyInviteResponse> inviteRecruiter(@PathVariable String companyId, @RequestBody CompanyInviteRequest request){
        return ApiResponse.<CompanyInviteResponse>builder()
                .result(companyService.inviteRecruiter(companyId, request))
                .build();
    }

    @GetMapping("/{companyId}/invites")
    public ApiResponse<List<CompanyInviteResponse>> getInvites(@PathVariable String companyId){
        return ApiResponse.<List<CompanyInviteResponse>>builder()
                .result(companyService.getInvites(companyId))
                .build();
    }

    @DeleteMapping("/{companyId}/invites/{inviteId}")
    public ApiResponse<CompanyInviteResponse> cancelInvite(@PathVariable String companyId, @PathVariable String inviteId){
        return ApiResponse.<CompanyInviteResponse>builder()
                .result(companyService.cancelInvite(companyId, inviteId))
                .build();
    }
}
