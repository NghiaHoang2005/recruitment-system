package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private UUID companyId;
    private String companyName;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private UUID cvId;
    private String cvName;
    private String cvUrl;
    private ApplicationStatus status;
    private Integer aiScore;
    private String coverLetter;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
}
