package com.recruitment.backend.domain.dtos.Operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLatencyResponse {
    private String requestType;
    private long totalRequests;
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double p50LatencyMs;
    private double p90LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private Map<String, Long> latencyBuckets;
}
