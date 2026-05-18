package com.recruitment.backend.notifications.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JobMatchNotificationRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String candidateName;

    @NotEmpty
    private List<String> matchedJobs;

    private String idempotencyKey;
}
