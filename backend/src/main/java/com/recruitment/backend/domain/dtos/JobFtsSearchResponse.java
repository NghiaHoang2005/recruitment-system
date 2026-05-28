package com.recruitment.backend.domain.dtos;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobFtsSearchResponse {
    private JobDTO job;
    private Double rank;
}
