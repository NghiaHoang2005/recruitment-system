package com.recruitment.backend.domain.dtos.Matching;

import com.recruitment.backend.domain.enums.MatchingRequestType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchingScoreSummaryResponse {
    private MatchingRequestType requestType;
    private int totalCount;
    private Double avgFitScore;
    private Double minFitScore;
    private Double maxFitScore;
    private Double p50FitScore;
    private Double p90FitScore;
    private Double avgLatencyMs;
    private Double p95LatencyMs;
    private Double maxLatencyMs;
    private List<MatchingScoreBucketResponse> scoreBuckets;
}
