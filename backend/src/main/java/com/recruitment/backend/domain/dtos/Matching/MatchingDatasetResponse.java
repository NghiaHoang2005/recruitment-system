package com.recruitment.backend.domain.dtos.Matching;

import com.recruitment.backend.domain.enums.MatchingDatasetDirection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MatchingDatasetResponse {
    private UUID id;
    private String name;
    private String description;
    private MatchingDatasetDirection direction;
    private UUID companyId;
    private Integer defaultTopK;
    private int pairCount;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
