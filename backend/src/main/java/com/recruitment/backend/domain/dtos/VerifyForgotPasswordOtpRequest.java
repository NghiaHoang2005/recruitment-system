package com.recruitment.backend.domain.dtos;

import lombok.Data;

@Data
public class VerifyForgotPasswordOtpRequest {
    private String email;
    private String otp;
}
