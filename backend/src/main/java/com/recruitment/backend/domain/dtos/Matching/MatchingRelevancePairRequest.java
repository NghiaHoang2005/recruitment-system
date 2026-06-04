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
public class MatchingRelevancePairRequest {
    private UUID cvId;
    private UUID jobId;
    private Integer relevance;
    private String notes;
}
