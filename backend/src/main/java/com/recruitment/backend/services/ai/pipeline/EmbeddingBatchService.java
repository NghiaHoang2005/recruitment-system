package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.AiProviderFallbackService;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingBatchService {

    private final AiProviderFallbackService providerFallbackService;
    private final AiProperties aiProperties;

    public EmbeddingResult embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("At least one text is required for embedding");
        }

        EmbeddingProvider provider = providerFallbackService.getEmbeddingProvider();
        int dimensions = aiProperties.getEmbedding().getRecommendedDimensions();
        int batchSize = effectiveBatchSize(texts.size());

        List<float[]> allVectors = new ArrayList<>(texts.size());
        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        long totalLatencyMs = 0L;

        String modelName = null;
        String modelVersion = null;
        String providerName = null;
        Integer resultDimensions = null;

        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> batch = texts.subList(start, end);

            EmbeddingResult batchResult = embedWithRetry(provider, batch, dimensions);
            if (batchResult.getVectors() == null || batchResult.getVectors().isEmpty()) {
                throw new IllegalStateException("Embedding result is empty for batch starting at index " + start);
            }

            allVectors.addAll(batchResult.getVectors());

            modelName = ensureConsistent("model", modelName, batchResult.getModelName());
            modelVersion = ensureConsistent("model version", modelVersion, batchResult.getModelVersion());
            providerName = ensureConsistent("provider", providerName, batchResult.getProvider());
            resultDimensions = ensureConsistent("dimensions", resultDimensions, batchResult.getDimensions());

            if (batchResult.getUsage() != null) {
                totalInputTokens += batchResult.getUsage().getInputTokens();
                totalOutputTokens += batchResult.getUsage().getOutputTokens();
                totalLatencyMs += batchResult.getUsage().getLatencyMs();
            }
        }

        if (allVectors.size() != texts.size()) {
            throw new IllegalStateException("Embedding vector count mismatch. inputs=" + texts.size()
                    + ", vectors=" + allVectors.size());
        }

        return EmbeddingResult.builder()
                .vectors(allVectors)
                .modelName(modelName != null ? modelName : aiProperties.getEmbedding().getModel())
                .modelVersion(modelVersion != null ? modelVersion : aiProperties.getEmbedding().getModel())
                .provider(providerName != null ? providerName : provider.providerName())
                .dimensions(resultDimensions != null ? resultDimensions : dimensions)
                .usage(AiUsage.builder()
                        .inputTokens(totalInputTokens)
                        .outputTokens(totalOutputTokens)
                        .latencyMs(totalLatencyMs)
                        .build())
                .build();
    }

    private EmbeddingResult embedWithRetry(EmbeddingProvider provider, List<String> texts, int dimensions) {
        int retries = aiProperties.getEmbedding().getRetries() == null ? 0 : aiProperties.getEmbedding().getRetries();
        int attempts = Math.max(1, retries + 1);
        Exception lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return provider.embed(EmbeddingRequest.builder()
                        .texts(texts)
                        .model(aiProperties.getEmbedding().getModel())
                        .dimensions(dimensions)
                        .timeoutMs(aiProperties.getEmbedding().getTimeoutMs())
                        .build());
            } catch (Exception ex) {
                lastException = ex;
                if (attempt < attempts) {
                    log.warn("Embedding batch failed (attempt {}/{}). Retrying: {}", attempt, attempts, ex.getMessage());
                    backoff(attempt);
                } else {
                    log.error("Embedding batch failed after {} attempts: {}", attempts, ex.getMessage());
                }
            }
        }

        throw new IllegalStateException("Embedding failed after " + attempts + " attempts", lastException);
    }

    private int effectiveBatchSize(int total) {
        Integer configured = aiProperties.getEmbedding().getBatchSize();
        if (configured == null || configured < 1) {
            return total;
        }
        return Math.min(configured, total);
    }

    private void backoff(int attempt) {
        long delayMs = 200L * attempt;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String ensureConsistent(String label, String current, String next) {
        if (current == null) {
            return next;
        }
        if (next != null && !current.equals(next)) {
            log.warn("Embedding batch returned inconsistent {} values: {} vs {}", label, current, next);
        }
        return current;
    }

    private Integer ensureConsistent(String label, Integer current, int next) {
        if (current == null) {
            return next;
        }
        if (current != next) {
            log.warn("Embedding batch returned inconsistent {} values: {} vs {}", label, current, next);
        }
        return current;
    }
}
