package com.recruitment.backend.controllers;

import com.recruitment.backend.domain.dtos.ApiResponse;
import com.recruitment.backend.domain.dtos.Operations.*;
import com.recruitment.backend.services.CacheManagementService;
import com.recruitment.backend.services.OperationsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/operations")
@RequiredArgsConstructor
public class OperationsDashboardController {

    private final OperationsDashboardService operationsDashboardService;
    private final CacheManagementService cacheManagementService;

    @GetMapping("/overview")
    public ApiResponse<OperationsOverviewResponse> getOverview() {
        return ApiResponse.success(operationsDashboardService.getOverview());
    }

    @GetMapping("/embeddings")
    public ApiResponse<EmbeddingStatsResponse> getEmbeddingStats() {
        return ApiResponse.success(operationsDashboardService.getEmbeddingStats());
    }

    @GetMapping("/search-latency")
    public ApiResponse<SearchLatencyResponse> getSearchLatency(
            @RequestParam(required = false) String requestType) {
        return ApiResponse.success(operationsDashboardService.getSearchLatency(requestType));
    }

    @GetMapping("/pipeline-health")
    public ApiResponse<PipelineHealthResponse> getPipelineHealth() {
        return ApiResponse.success(operationsDashboardService.getPipelineHealth());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertResponse>> getAlerts() {
        return ApiResponse.success(operationsDashboardService.getAlerts());
    }

    @GetMapping("/cache-stats")
    public ApiResponse<Map<String, Object>> getCacheStats() {
        return ApiResponse.success(cacheManagementService.getCacheStats());
    }

    @PostMapping("/cache/evict-all")
    public ApiResponse<String> evictAllCaches() {
        cacheManagementService.evictAllMatchCaches();
        cacheManagementService.evictOperationsCaches();
        return ApiResponse.success("All caches evicted");
    }
}
