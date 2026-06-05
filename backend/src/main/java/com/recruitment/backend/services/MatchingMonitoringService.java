package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.Matching.MatchingScoreBucketResponse;
import com.recruitment.backend.domain.dtos.Matching.MatchingScoreSummaryResponse;
import com.recruitment.backend.domain.entities.Matching.MatchingScoreEvent;
import com.recruitment.backend.domain.enums.MatchingRequestType;
import com.recruitment.backend.repositories.MatchingScoreEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingMonitoringService {

    private final MatchingScoreEventRepository scoreEventRepository;

    @Async
    @Transactional
    public void recordMatchingEvent(
            MatchingRequestType requestType,
            UUID companyId,
            UUID jobId,
            UUID cvId,
            Double fitScore,
            Double semanticScore,
            Double ftsScore,
            Double skillScore,
            Long latencyMs
    ) {
        MatchingScoreEvent event = MatchingScoreEvent.builder()
                .requestType(requestType)
                .companyId(companyId)
                .jobId(jobId)
                .cvId(cvId)
                .fitScore(fitScore)
                .semanticScore(semanticScore)
                .ftsScore(ftsScore)
                .skillScore(skillScore)
                .latencyMs(latencyMs)
                .build();

        scoreEventRepository.save(event);
    }

    public MatchingScoreSummaryResponse getScoreSummary(MatchingRequestType requestType, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<MatchingScoreEvent> events = scoreEventRepository.findByRequestTypeOrderByCreatedAtDesc(
                requestType,
                pageable
        );

        if (events.isEmpty()) {
            return MatchingScoreSummaryResponse.builder()
                    .requestType(requestType)
                    .totalCount(0)
                    .avgFitScore(0.0)
                    .minFitScore(0.0)
                    .maxFitScore(0.0)
                    .p50FitScore(0.0)
                    .p90FitScore(0.0)
                    .avgLatencyMs(0.0)
                    .p95LatencyMs(0.0)
                    .maxLatencyMs(0.0)
                    .scoreBuckets(List.of())
                    .build();
        }

        List<MatchingScoreEvent> eventList = events.getContent();
        List<Double> fitScores = eventList.stream()
                .map(MatchingScoreEvent::getFitScore)
                .filter(s -> s != null)
                .sorted()
                .collect(Collectors.toList());

        List<Long> latencies = eventList.stream()
                .map(MatchingScoreEvent::getLatencyMs)
                .filter(l -> l != null)
                .sorted()
                .collect(Collectors.toList());

        DoubleSummaryStatistics fitStats = fitScores.stream()
                .collect(Collectors.summarizingDouble(Double::doubleValue));

        LongSummaryStatistics latencyStats = latencies.stream()
                .collect(Collectors.summarizingLong(Long::longValue));

        double p50FitScore = fitScores.isEmpty() ? 0.0 : fitScores.get(fitScores.size() / 2);
        double p90FitScore = fitScores.isEmpty() ? 0.0 : fitScores.get((int) (fitScores.size() * 0.9));
        long p95LatencyMs = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.95));

        List<MatchingScoreBucketResponse> buckets = buildScoreBuckets(fitScores);

        return MatchingScoreSummaryResponse.builder()
                .requestType(requestType)
                .totalCount(eventList.size())
                .avgFitScore(fitStats.getAverage())
                .minFitScore(fitStats.getMin())
                .maxFitScore(fitStats.getMax())
                .p50FitScore(p50FitScore)
                .p90FitScore(p90FitScore)
                .avgLatencyMs(latencyStats.getCount() > 0 ? (double) latencyStats.getSum() / latencyStats.getCount() : 0.0)
                .p95LatencyMs((double) p95LatencyMs)
                .maxLatencyMs((double) latencyStats.getMax())
                .scoreBuckets(buckets)
                .build();
    }

    private List<MatchingScoreBucketResponse> buildScoreBuckets(List<Double> scores) {
        List<MatchingScoreBucketResponse> buckets = new ArrayList<>();
        double[] ranges = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};

        for (int i = 0; i < ranges.length - 1; i++) {
            double min = ranges[i];
            double max = ranges[i + 1];
            long count = scores.stream()
                    .filter(s -> s >= min && s < max)
                    .count();

            buckets.add(MatchingScoreBucketResponse.builder()
                    .min(min)
                    .max(max)
                    .count(count)
                    .build());
        }

        return buckets;
    }
}
