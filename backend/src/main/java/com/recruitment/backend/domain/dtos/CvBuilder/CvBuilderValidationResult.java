package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CvBuilderValidationResult {
    private CvBuilderValidationStatus status;
    private List<CvBuilderValidationIssue> issues;
    private LocalDateTime validatedAt;
}
