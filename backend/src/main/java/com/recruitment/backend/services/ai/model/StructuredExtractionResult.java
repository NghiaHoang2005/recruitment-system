package com.recruitment.backend.services.ai.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StructuredExtractionResult {
    private final String json;
    private final String modelName;
    private final String modelVersion;
    private final String provider;
    private final AiUsage usage;
}
