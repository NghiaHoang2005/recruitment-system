package com.recruitment.backend.domain.dtos.Matching;

import com.recruitment.backend.domain.enums.MatchingDatasetDirection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MatchingEvaluationResponse {
    private UUID id;
    private UUID companyId;
    private UUID datasetId;
    private String datasetName;
    private MatchingDatasetDirection direction;
    private Integer topK;
    private Integer evaluatedPairs;
    private Integer successfulPairs;
    private String status;
    private Double precisionAtK;
    private Double recallAtK;
    private Double f1AtK;
    private Integer totalQueries;
    private Integer totalRelevant;
    private UUID weightProfileId;
    private String weightVersion;
    private String version;
    private String errorMessage;
    private LocalDateTime evaluatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
