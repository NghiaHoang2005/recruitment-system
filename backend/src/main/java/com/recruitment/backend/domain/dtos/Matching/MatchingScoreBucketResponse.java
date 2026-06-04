package com.recruitment.backend.domain.dtos.Matching;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingScoreBucketResponse {
    private double min;
    private double max;
    private long count;
}
