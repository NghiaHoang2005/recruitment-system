package com.recruitment.backend.controllers;

import com.nimbusds.jose.JOSEException;
import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.enums.AccountType;
import com.recruitment.backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/request-otp")
    public ResponseEntity<ApiResponse<OtpSentResponse>> requestRegisterOtp(@RequestBody RegisterOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestRegisterOtp(request)));
    }
    @PostMapping("/register/resend-otp")
    public ResponseEntity<ApiResponse<OtpSentResponse>> resendRegisterOtp(@RequestBody RegisterOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestRegisterOtp(request)));
    }

    @PostMapping("/register/candidate")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCandidate(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request, AccountType.CANDIDATE)));
    }

    @PostMapping("/register/recruiter")
    public ResponseEntity<ApiResponse<AuthResponse>> registerRecruiter(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request, AccountType.RECRUITER)));
    }


    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.authenticate(request)));
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<ApiResponse<OtpSentResponse>> requestForgotPasswordOtp(@RequestBody ForgotPasswordOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestForgotPasswordOtp(request)));
    }
    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<ApiResponse<OtpSentResponse>> resendForgotPasswordOtp(@RequestBody ForgotPasswordOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestForgotPasswordOtp(request)));
    }
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyForgotPasswordOtp(@RequestBody VerifyForgotPasswordOtpRequest request) {
        String resetToken = authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success(resetToken));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .message("Success")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody RefreshRequest request) throws ParseException, JOSEException {
        return ApiResponse.<AuthResponse>builder()
                .result(authService.refreshToken(request))
                .build();
    }
}
