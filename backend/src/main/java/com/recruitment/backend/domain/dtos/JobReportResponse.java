package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.JobReportReason;
import com.recruitment.backend.domain.enums.JobReportStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobReportResponse {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private String jobStatus;
    private UUID reporterId;
    private String reporterEmail;
    private JobReportReason reason;
    private String details;
    private JobReportStatus status;
    private String adminNote;
    private String reviewedByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
