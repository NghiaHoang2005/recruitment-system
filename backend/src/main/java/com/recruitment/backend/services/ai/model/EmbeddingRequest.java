package com.recruitment.backend.services.ai.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmbeddingRequest {
    private final List<String> texts;
    private final String model;
    private final int dimensions;
    private final int timeoutMs;
}
