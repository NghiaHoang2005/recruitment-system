package com.recruitment.backend.services.ai.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StructuredExtractionRequest {
    private final String text;
    private final String prompt;
    private final String schema;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final int timeoutMs;
}
