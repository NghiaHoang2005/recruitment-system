package com.recruitment.backend.services.ai.providers.impl;

import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockEmbeddingProvider implements EmbeddingProvider {

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : request.getTexts()) {
            vectors.add(deterministicVector(text, request.getDimensions()));
        }

        int inputTokens = request.getTexts().stream()
                .mapToInt(text -> Math.max(1, text.length() / 4))
                .sum();

        return EmbeddingResult.builder()
                .vectors(vectors)
                .modelName(request.getModel())
                .modelVersion("mock-v1")
                .provider(providerName())
                .dimensions(request.getDimensions())
                .usage(AiUsage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(0)
                        .latencyMs(5)
                        .build())
                .build();
    }

    private float[] deterministicVector(String text, int dimensions) {
        float[] result = new float[dimensions];
        int seed = text == null ? 0 : text.hashCode();
        for (int i = 0; i < dimensions; i++) {
            int value = seed ^ (i * 31);
            result[i] = (value % 1000) / 1000.0f;
        }
        return result;
    }
}
