package com.recruitment.backend.domain.dtos.Cv;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CvSemanticSearchResponse {
    private CvItemResponse cv;
    private Double distance;
    private Double similarity;
}
