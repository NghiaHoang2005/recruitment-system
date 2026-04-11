package com.recruitment.backend.services.ai.providers;

import com.recruitment.backend.services.ai.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<String, TextExtractionProvider> extractionProviders;
    private final Map<String, EmbeddingProvider> embeddingProviders;
    private final AiProperties aiProperties;

    public ProviderRegistry(
            List<TextExtractionProvider> textExtractionProviders,
            List<EmbeddingProvider> embeddingProviders,
            AiProperties aiProperties
    ) {
        this.extractionProviders = textExtractionProviders.stream()
                .collect(Collectors.toMap(p -> p.providerName().toLowerCase(), Function.identity()));
        this.embeddingProviders = embeddingProviders.stream()
                .collect(Collectors.toMap(p -> p.providerName().toLowerCase(), Function.identity()));
        this.aiProperties = aiProperties;
    }

    public TextExtractionProvider getExtractionProvider() {
        String providerName = aiProperties.getExtraction().getActiveProvider().toLowerCase();
        TextExtractionProvider provider = extractionProviders.get(providerName);
        if (provider == null) {
            throw new IllegalStateException("Text extraction provider not found: " + providerName);
        }
        return provider;
    }

    public EmbeddingProvider getEmbeddingProvider() {
        String providerName = aiProperties.getEmbedding().getActiveProvider().toLowerCase();
        EmbeddingProvider provider = embeddingProviders.get(providerName);
        if (provider == null) {
            throw new IllegalStateException("Embedding provider not found: " + providerName);
        }
        return provider;
    }

    public TextExtractionProvider getExtractionProviderByName(String providerName) {
        String normalizedName = providerName.toLowerCase();
        TextExtractionProvider provider = extractionProviders.get(normalizedName);
        if (provider == null) {
            throw new IllegalStateException("Text extraction provider not found: " + providerName);
        }
        return provider;
    }

    public EmbeddingProvider getEmbeddingProviderByName(String providerName) {
        String normalizedName = providerName.toLowerCase();
        EmbeddingProvider provider = embeddingProviders.get(normalizedName);
        if (provider == null) {
            throw new IllegalStateException("Embedding provider not found: " + providerName);
        }
        return provider;
    }
}
