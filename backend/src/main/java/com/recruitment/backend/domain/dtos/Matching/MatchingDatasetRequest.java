package com.recruitment.backend.domain.dtos.Matching;

import com.recruitment.backend.domain.enums.MatchingDatasetDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingDatasetRequest {
    private String name;
    private String description;
    private MatchingDatasetDirection direction;
    private UUID companyId;
    private Integer defaultTopK;
    private List<MatchingRelevancePairRequest> pairs;
}
