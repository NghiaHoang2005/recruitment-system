package com.recruitment.backend.domain.dtos.Operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingStatsResponse {
    private long totalCvEmbeddings;
    private long totalJobEmbeddings;
    private Map<String, Long> cvEmbeddingsByType;
    private Map<String, Long> jobEmbeddingsByType;
    private List<String> models;
    private List<Integer> dimensions;
}
