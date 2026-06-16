package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.JobReportReason;
import lombok.Data;

@Data
public class JobReportRequest {
    private JobReportReason reason;
    private String details;
}
