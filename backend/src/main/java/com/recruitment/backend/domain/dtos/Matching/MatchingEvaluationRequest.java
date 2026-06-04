package com.recruitment.backend.domain.dtos.Matching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingEvaluationRequest {
    private UUID datasetId;
    private Integer topK;
    private UUID weightProfileId;
}
