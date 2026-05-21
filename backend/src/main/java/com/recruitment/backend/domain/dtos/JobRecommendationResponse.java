package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobRecommendationResponse {
    private JobDTO job;
    private Integer matchScore;
}
