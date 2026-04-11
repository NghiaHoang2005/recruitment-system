package com.recruitment.backend.services.ai.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmbeddingResult {
    private final List<float[]> vectors;
    private final String modelName;
    private final String modelVersion;
    private final String provider;
    private final int dimensions;
    private final AiUsage usage;
}
