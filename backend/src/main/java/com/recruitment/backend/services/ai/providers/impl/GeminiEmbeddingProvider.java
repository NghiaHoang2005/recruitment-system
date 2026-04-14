package com.recruitment.backend.services.ai.providers.impl;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
    name = "ai.embedding.active-provider",
    havingValue = "gemini",
    matchIfMissing = false
)
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private final AiProperties aiProperties;
    private final EmbeddingModel embeddingModel;

    public GeminiEmbeddingProvider(
            AiProperties aiProperties,
            EmbeddingModel embeddingModel
    ) {
        this.aiProperties = aiProperties;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            if (request.getTexts() == null || request.getTexts().isEmpty()) {
                throw new IllegalArgumentException("At least one text is required for embedding");
            }

            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(request.getModel())
                    .dimensions(request.getDimensions())
                    .build();

            List<float[]> vectors = new java.util.ArrayList<>(request.getTexts().size());
            int totalPromptTokens = 0;

            for (int i = 0; i < request.getTexts().size(); i++) {
                String originalText = request.getTexts().get(i);
                String text = normalizeInput(originalText);

                log.debug("Embedding item {}/{} ({} chars)", i + 1, request.getTexts().size(), text.length());

                EmbeddingResponse singleResponse = embeddingModel.call(
                        new org.springframework.ai.embedding.EmbeddingRequest(List.of(text), options)
                );

                if (singleResponse.getResults() == null || singleResponse.getResults().isEmpty()) {
                    throw new IllegalStateException("No embedding vector returned for item index " + i);
                }

                float[] vector = singleResponse.getResults().get(0).getOutput();
                vectors.add(vector);

                Integer promptTokens = singleResponse.getMetadata() != null
                        && singleResponse.getMetadata().getUsage() != null
                        ? singleResponse.getMetadata().getUsage().getPromptTokens()
                        : null;
                totalPromptTokens += promptTokens != null ? promptTokens : 0;
            }

            if (vectors.size() != request.getTexts().size()) {
                throw new IllegalStateException(
                        "Embedding vector count mismatch. requested=" + request.getTexts().size()
                                + ", returned=" + vectors.size());
            }

            long latency = System.currentTimeMillis() - startTime;

            log.info("Gemini embedding successful. texts={}, totalTokens={}, latencyMs={}",
                    request.getTexts().size(), totalPromptTokens, latency);

            return EmbeddingResult.builder()
                    .vectors(vectors)
                    .modelName(request.getModel())
                    .modelVersion(request.getModel())
                    .provider(providerName())
                    .dimensions(vectors.isEmpty() ? 0 : vectors.get(0).length)
                    .usage(AiUsage.builder()
                            .inputTokens(totalPromptTokens)
                            .outputTokens(0)
                            .latencyMs(latency)
                            .build())
                    .build();

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini embedding failed after {}ms: {}", latency, e.getMessage(), e);
            throw new IllegalStateException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return "";
        }

        String trimmed = text.trim();
        int maxChars = aiProperties.getEmbedding().getMaxCharactersPerInput();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }

        log.warn("Embedding input exceeds max chars ({} > {}), truncating", trimmed.length(), maxChars);
        return trimmed.substring(0, maxChars);
    }
}
