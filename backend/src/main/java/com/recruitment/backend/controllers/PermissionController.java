package com.recruitment.backend.controllers;


import com.cloudinary.Api;
import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.PermissionRequest;
import com.recruitment.backend.domain.dtos.PermissionResponse;
import com.recruitment.backend.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping
    public ApiResponse<PermissionResponse> create(@RequestBody PermissionRequest request){
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .message("Success")
                .build();
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAll(){
        return ApiResponse.<List<PermissionResponse>>builder()
                .message("Success")
                .result(permissionService.getAll())
                .build();
    }

    @DeleteMapping("/{permission}")
    public ApiResponse<Void> delete(@PathVariable String permission){
        permissionService.delete(permission);
        return ApiResponse.<Void>builder().build();
    }
}
