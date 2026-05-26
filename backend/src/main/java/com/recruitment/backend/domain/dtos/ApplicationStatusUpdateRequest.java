package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.ApplicationStatus;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    private ApplicationStatus status;
}
