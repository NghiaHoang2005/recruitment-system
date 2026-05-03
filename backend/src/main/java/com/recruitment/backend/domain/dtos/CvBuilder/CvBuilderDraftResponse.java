package com.recruitment.backend.domain.dtos.CvBuilder;

import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderDraftStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CvBuilderDraftResponse {
    private UUID id;
    private UUID templateId;
    private String templateCode;
    private String templateName;
    private UUID sourceCvId;
    private String title;
    private String contentJson;
    private CvBuilderDraftStatus status;
    private CvBuilderValidationStatus validationStatus;
    private List<CvBuilderValidationIssue> validationIssues;
    private LocalDateTime validatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
