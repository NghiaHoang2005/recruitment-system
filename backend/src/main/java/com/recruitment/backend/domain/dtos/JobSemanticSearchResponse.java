package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobSemanticSearchResponse {
    private JobDTO job;
    private Double distance;
    private Double similarity;
}
