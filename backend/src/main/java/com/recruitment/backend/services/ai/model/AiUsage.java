package com.recruitment.backend.services.ai.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiUsage {
    private int inputTokens;
    private int outputTokens;
    private long latencyMs;
}
