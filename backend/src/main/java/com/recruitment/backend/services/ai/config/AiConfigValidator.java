package com.recruitment.backend.services.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiConfigValidator implements ApplicationRunner {

    private final AiProperties aiProperties;

    public AiConfigValidator(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== AI Pipeline Configuration Validation ===");
        
        // Check extraction provider
        String extractionProvider = aiProperties.getExtraction().getActiveProvider();
        log.info("Text Extraction Provider: {}", extractionProvider);
        log.info("  - Model: {}", aiProperties.getExtraction().getModel());
        log.info("  - Temperature: {}", aiProperties.getExtraction().getTemperature());
        log.info("  - Max Tokens: {}", aiProperties.getExtraction().getMaxTokens());
        
        if ("openai".equalsIgnoreCase(extractionProvider)) {
            log.warn("⚠ OpenAI provider is no longer supported. Using FREE Gemini instead.");
            log.warn("Please set AI_EXTRACTION_PROVIDER=gemini or use mock for testing.");
        } else if ("gemini".equalsIgnoreCase(extractionProvider)) {
            validateGeminiApiKey();
            log.info("✓ Gemini extraction provider configured (FREE - 60 req/min)");
        } else if ("mock".equalsIgnoreCase(extractionProvider)) {
            log.info("ℹ Mock extraction provider configured (non-production)");
        } else {
            log.warn("⚠ Unknown extraction provider: {}", extractionProvider);
        }

        // Check embedding provider
        String embeddingProvider = aiProperties.getEmbedding().getActiveProvider();
        log.info("Embedding Provider: {}", embeddingProvider);
        log.info("  - Model: {}", aiProperties.getEmbedding().getModel());
        log.info("  - Dimensions: {}", aiProperties.getEmbedding().getDimensions());
        log.info("  - Batch Size: {}", aiProperties.getEmbedding().getBatchSize());
        
        if ("openai".equalsIgnoreCase(embeddingProvider)) {
            log.warn("⚠ OpenAI provider is no longer supported. Using FREE Gemini instead.");
            log.warn("Please set AI_EMBEDDING_PROVIDER=gemini or use mock for testing.");
        } else if ("gemini".equalsIgnoreCase(embeddingProvider)) {
            validateGeminiApiKey();
            log.info("✓ Gemini embedding provider configured (FREE - 100 req/min)");
        } else if ("mock".equalsIgnoreCase(embeddingProvider)) {
            log.info("ℹ Mock embedding provider configured (non-production)");
        } else {
            log.warn("⚠ Unknown embedding provider: {}", embeddingProvider);
        }

        log.info("=== AI Configuration Validation Complete ===");
    }

    private void validateOpenAiApiKey() {
        log.warn("OpenAI provider was detected but is no longer supported in this deployment.");
    }

    private void validateGeminiApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini provider is enabled but GEMINI_API_KEY environment variable is not set. " +
                    "Please set GEMINI_API_KEY before starting the application. " +
                    "Get free API key at: https://aistudio.google.com/app/apikey"
            );
        }

        // Gemini API keys are typically longer
        if (apiKey.length() < 20) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY appears to be invalid (too short). " +
                    "Get a valid free key at: https://aistudio.google.com/app/apikey"
            );
        }

        log.info("✓ GEMINI_API_KEY validation passed (key length: {}). " +
                 "Using FREE Gemini tier (60 req/min)", apiKey.length());
    }
}
