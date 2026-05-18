package com.recruitment.backend.notifications.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendOtpNotificationRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String otpCode;

    private Integer ttlMinutes;

    private String idempotencyKey;
}
