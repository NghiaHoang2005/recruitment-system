package com.recruitment.backend.domain.dtos.Operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationsOverviewResponse {
    private long totalCvEmbeddings;
    private long totalJobEmbeddings;
    private long activePipelineJobs;
    private long queuedPipelineJobs;
    private long failedPipelineJobs;
    private long completedPipelineJobs;
    private double avgSearchLatencyMs;
    private double p95SearchLatencyMs;
    private long totalSearchRequests;
    private List<String> cacheNames;
    private int totalCaches;
}
