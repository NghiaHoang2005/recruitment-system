package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.JobReportStatus;
import lombok.Data;

@Data
public class JobReportReviewRequest {
    private JobReportStatus status;
    private String adminNote;
}
