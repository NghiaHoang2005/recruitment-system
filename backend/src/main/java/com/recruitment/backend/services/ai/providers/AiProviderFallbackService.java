package com.recruitment.backend.services.ai.providers;

import com.recruitment.backend.services.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiProviderFallbackService {

    private final ProviderRegistry providerRegistry;
    private final AiProperties aiProperties;

    public AiProviderFallbackService(ProviderRegistry providerRegistry, AiProperties aiProperties) {
        this.providerRegistry = providerRegistry;
        this.aiProperties = aiProperties;
    }

    public TextExtractionProvider getExtractionProvider() {
        String primaryProvider = aiProperties.getExtraction().getActiveProvider().toLowerCase();
        
        try {
            TextExtractionProvider provider = providerRegistry.getExtractionProvider();
            log.debug("Using text extraction provider: {}", primaryProvider);
            return provider;
        } catch (IllegalStateException e) {
            log.warn("Primary text extraction provider '{}' not available: {}. Falling back to mock provider.", 
                    primaryProvider, e.getMessage());
            
            // Fallback to mock provider
            try {
                return providerRegistry.getExtractionProviderByName("mock");
            } catch (Exception fallbackError) {
                log.error("Fallback to mock text extraction provider failed", fallbackError);
                throw new IllegalStateException(
                        "No text extraction provider available. Primary: " + primaryProvider + ", Fallback: mock", 
                        fallbackError
                );
            }
        }
    }

    public EmbeddingProvider getEmbeddingProvider() {
        String primaryProvider = aiProperties.getEmbedding().getActiveProvider().toLowerCase();
        
        try {
            EmbeddingProvider provider = providerRegistry.getEmbeddingProvider();
            log.debug("Using embedding provider: {}", primaryProvider);
            return provider;
        } catch (IllegalStateException e) {
            log.warn("Primary embedding provider '{}' not available: {}. Falling back to mock provider.", 
                    primaryProvider, e.getMessage());
            
            // Fallback to mock provider
            try {
                return providerRegistry.getEmbeddingProviderByName("mock");
            } catch (Exception fallbackError) {
                log.error("Fallback to mock embedding provider failed", fallbackError);
                throw new IllegalStateException(
                        "No embedding provider available. Primary: " + primaryProvider + ", Fallback: mock", 
                        fallbackError
                );
            }
        }
    }
}
