package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Operations.*;
import com.recruitment.backend.domain.enums.MatchingRequestType;
import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.repositories.CvEmbeddingRepository;
import com.recruitment.backend.repositories.JobEmbeddingRepository;
import com.recruitment.backend.repositories.MatchingScoreEventRepository;
import com.recruitment.backend.repositories.PipelineJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationsDashboardService {

    private final CvEmbeddingRepository cvEmbeddingRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final MatchingScoreEventRepository scoreEventRepository;
    private final PipelineJobRepository pipelineJobRepository;
    private final CacheManagementService cacheManagementService;

    @SuppressWarnings("unchecked")
    public OperationsOverviewResponse getOverview() {
        long totalCvEmb = cvEmbeddingRepository.count();
        long totalJobEmb = jobEmbeddingRepository.count();

        long activeJobs = pipelineJobRepository.countByStatus(PipelineJobStatus.RUNNING);
        long queuedJobs = pipelineJobRepository.countByStatus(PipelineJobStatus.QUEUED);
        long failedJobs = pipelineJobRepository.countByStatus(PipelineJobStatus.FAILED);
        long completedJobs = pipelineJobRepository.countByStatus(PipelineJobStatus.COMPLETED);

        Double avgLatency = scoreEventRepository.findOverallAvgLatency();
        List<Long> latencies = scoreEventRepository.findAllLatenciesOrderedAsc();
        double p95 = computePercentile(latencies, 0.95);

        Map<String, Object> cacheStats = cacheManagementService.getCacheStats();

        return OperationsOverviewResponse.builder()
                .totalCvEmbeddings(totalCvEmb)
                .totalJobEmbeddings(totalJobEmb)
                .activePipelineJobs(activeJobs)
                .queuedPipelineJobs(queuedJobs)
                .failedPipelineJobs(failedJobs)
                .completedPipelineJobs(completedJobs)
                .avgSearchLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .p95SearchLatencyMs(p95)
                .totalSearchRequests(scoreEventRepository.count())
                .cacheNames((List<String>) cacheStats.getOrDefault("cacheNames", List.of()))
                .totalCaches((int) cacheStats.getOrDefault("totalCaches", 0))
                .build();
    }

    public EmbeddingStatsResponse getEmbeddingStats() {
        long totalCv = cvEmbeddingRepository.count();
        long totalJob = jobEmbeddingRepository.count();

        return EmbeddingStatsResponse.builder()
                .totalCvEmbeddings(totalCv)
                .totalJobEmbeddings(totalJob)
                .cvEmbeddingsByType(Map.of())
                .jobEmbeddingsByType(Map.of())
                .models(List.of())
                .dimensions(List.of())
                .build();
    }

    public SearchLatencyResponse getSearchLatency(String requestType) {
        MatchingRequestType type = null;
        try {
            if (requestType != null && !requestType.isBlank()) {
                type = MatchingRequestType.valueOf(requestType.toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {}

        List<Long> latencies;
        long totalRequests;
        Double avgLatency;

        if (type != null) {
            latencies = scoreEventRepository.findAllLatenciesByTypeOrderedAsc(type);
            totalRequests = scoreEventRepository.countByRequestType(type);
            avgLatency = scoreEventRepository.findAvgLatencyByType(type);
        } else {
            latencies = scoreEventRepository.findAllLatenciesOrderedAsc();
            totalRequests = scoreEventRepository.count();
            avgLatency = scoreEventRepository.findOverallAvgLatency();
        }

        Map<String, Long> buckets = buildLatencyBuckets(latencies);

        return SearchLatencyResponse.builder()
                .requestType(requestType != null ? requestType : "ALL")
                .totalRequests(totalRequests)
                .avgLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .minLatencyMs(latencies.isEmpty() ? 0 : latencies.get(0))
                .maxLatencyMs(latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1))
                .p50LatencyMs(computePercentile(latencies, 0.50))
                .p90LatencyMs(computePercentile(latencies, 0.90))
                .p95LatencyMs(computePercentile(latencies, 0.95))
                .p99LatencyMs(computePercentile(latencies, 0.99))
                .latencyBuckets(buckets)
                .build();
    }

    public PipelineHealthResponse getPipelineHealth() {
        long active = pipelineJobRepository.countByStatus(PipelineJobStatus.RUNNING);
        long queued = pipelineJobRepository.countByStatus(PipelineJobStatus.QUEUED);
        long completed = pipelineJobRepository.countByStatus(PipelineJobStatus.COMPLETED);
        long failed = pipelineJobRepository.countByStatus(PipelineJobStatus.FAILED);
        long stalled = pipelineJobRepository.countStalledJobs(LocalDateTime.now().minusMinutes(30));

        LocalDateTime lastCompletion = pipelineJobRepository.findLatestCompletionTime();

        var recentJobs = pipelineJobRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(job -> PipelineHealthResponse.PipelineJobSummary.builder()
                        .id(job.getId())
                        .jobType(job.getJobType().name())
                        .status(job.getStatus().name())
                        .totalItems(job.getTotalItems())
                        .processedItems(job.getProcessedItems())
                        .failedItems(job.getFailedItems())
                        .startedAt(job.getStartedAt())
                        .completedAt(job.getCompletedAt())
                        .createdAt(job.getCreatedAt())
                        .errorMessage(job.getErrorMessage())
                        .build())
                .collect(Collectors.toList());

        return PipelineHealthResponse.builder()
                .activeJobs(active)
                .queuedJobs(queued)
                .completedJobs(completed)
                .failedJobs(failed)
                .stalledJobs(stalled)
                .lastCompletionTime(lastCompletion)
                .recentJobs(recentJobs)
                .build();
    }

    public List<AlertResponse> getAlerts() {
        List<AlertResponse> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        long stalled = pipelineJobRepository.countStalledJobs(now.minusMinutes(30));
        if (stalled > 0) {
            alerts.add(AlertResponse.builder()
                    .severity("CRITICAL")
                    .type("STALLED_PIPELINE")
                    .message(stalled + " pipeline job(s) đã chạy quá 30 phút")
                    .details("Kiểm tra pipeline jobs và restart nếu cần")
                    .detectedAt(now)
                    .build());
        }

        List<Long> latencies = scoreEventRepository.findAllLatenciesOrderedAsc();
        double p95 = computePercentile(latencies, 0.95);
        if (p95 > 5000) {
            alerts.add(AlertResponse.builder()
                    .severity("WARNING")
                    .type("DEGRADED_LATENCY")
                    .message("P95 search latency vượt ngưỡng: " + String.format("%.0f", p95) + "ms")
                    .details("Ngưỡng cho phép: 5000ms. Xem xét re-index hoặc scale up")
                    .detectedAt(now)
                    .build());
        }

        long failedJobs = pipelineJobRepository.countByStatus(PipelineJobStatus.FAILED);
        long totalJobs = pipelineJobRepository.count();
        if (totalJobs > 0 && (double) failedJobs / totalJobs > 0.3) {
            alerts.add(AlertResponse.builder()
                    .severity("WARNING")
                    .type("HIGH_ERROR_RATE")
                    .message("Tỷ lệ pipeline thất bại cao: " + failedJobs + "/" + totalJobs)
                    .details("Kiểm tra logs để tìm nguyên nhân")
                    .detectedAt(now)
                    .build());
        }

        return alerts;
    }

    private double computePercentile(List<Long> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private Map<String, Long> buildLatencyBuckets(List<Long> latencies) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        long[] thresholds = {100, 500, 1000, 2000, 5000};
        String[] labels = {"0-100ms", "100-500ms", "500-1000ms", "1000-2000ms", "2000-5000ms", "5000ms+"};

        long[] counts = new long[labels.length];
        for (Long latency : latencies) {
            if (latency == null) continue;
            boolean placed = false;
            for (int i = 0; i < thresholds.length; i++) {
                if (latency <= thresholds[i]) {
                    counts[i]++;
                    placed = true;
                    break;
                }
            }
            if (!placed) counts[labels.length - 1]++;
        }

        for (int i = 0; i < labels.length; i++) {
            buckets.put(labels[i], counts[i]);
        }
        return buckets;
    }
}
