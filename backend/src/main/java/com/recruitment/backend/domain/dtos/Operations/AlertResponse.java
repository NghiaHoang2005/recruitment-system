package com.recruitment.backend.domain.dtos.Operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private String severity;
    private String type;
    private String message;
    private String details;
    private LocalDateTime detectedAt;
}
