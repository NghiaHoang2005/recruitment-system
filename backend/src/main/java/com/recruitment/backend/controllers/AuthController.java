package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.AuthRequest;
import com.recruitment.backend.domain.dtos.AuthResponse;
import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.RegisterRequest;
import com.recruitment.backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.authenticate(request)));
    }
}
