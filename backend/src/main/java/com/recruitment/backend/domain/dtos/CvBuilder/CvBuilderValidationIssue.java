package com.recruitment.backend.domain.dtos.CvBuilder;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CvBuilderValidationIssue {
    private String code;
    private CvBuilderValidationSeverity severity;
    private String sectionId;
    private String fieldPath;
    private String message;
    private String hint;
}
