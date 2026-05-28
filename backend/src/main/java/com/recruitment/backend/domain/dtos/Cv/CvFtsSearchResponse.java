package com.recruitment.backend.domain.dtos.Cv;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CvFtsSearchResponse {
    private CvItemResponse cv;
    private Double rank;
}
