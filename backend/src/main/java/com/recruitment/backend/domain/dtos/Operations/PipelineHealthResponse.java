package com.recruitment.backend.domain.dtos.Operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineHealthResponse {
    private long activeJobs;
    private long queuedJobs;
    private long completedJobs;
    private long failedJobs;
    private long stalledJobs;
    private LocalDateTime lastCompletionTime;
    private List<PipelineJobSummary> recentJobs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineJobSummary {
        private java.util.UUID id;
        private String jobType;
        private String status;
        private int totalItems;
        private int processedItems;
        private int failedItems;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;
        private String errorMessage;
    }
}
