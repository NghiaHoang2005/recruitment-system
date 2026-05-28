package com.recruitment.backend.domain.dtos.Cv;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CvRecommendationResponse {
    private CvItemResponse cv;
    private Integer matchScore;
}
