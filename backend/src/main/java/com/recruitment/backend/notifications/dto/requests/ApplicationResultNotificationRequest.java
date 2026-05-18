package com.recruitment.backend.notifications.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationResultNotificationRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String candidateName;

    @NotBlank
    private String jobTitle;

    @NotNull
    private Boolean passed;

    private String feedback;

    private String idempotencyKey;
}
