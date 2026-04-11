package com.recruitment.backend.services.ai.providers.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Gemini Embedding Provider (FREE TIER)
 * 
 * Note: These tests require:
 * 1. GEMINI_API_KEY environment variable to be set (get free at https://aistudio.google.com/app/apikey)
 * 2. AI_EMBEDDING_PROVIDER=gemini configuration
 * 
 * These tests will be SKIPPED if GEMINI_API_KEY is not set.
 * Gemini embedding API: embedding-001, 768 dimensions, 100 requests/min free tier
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@DisplayName("Gemini Embedding Provider Integration Tests")
public class GeminiEmbeddingProviderIntegrationTest {

    @Autowired(required = false)
    private GeminiEmbeddingProvider provider;

    @Autowired
    private AiProperties aiProperties;

    @Test
    @DisplayName("Provider should be configured when GEMINI_API_KEY is set")
    public void testProviderAvailable() {
        assertNotNull(provider, "Gemini embedding provider should be available when GEMINI_API_KEY is set");
        assertEquals("gemini", provider.providerName());
    }

    @Test
    @DisplayName("Should generate embedding for text successfully using Gemini free tier")
    public void testEmbedSingleText() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Arrange
        String text = "Senior Software Engineer with 5 years experience in Java and Spring Boot";
        List<String> texts = Arrays.asList(text);

        EmbeddingRequest request = EmbeddingRequest.builder()
                .texts(texts)
                .model("embedding-001")
                .dimensions(768)
                .build();

        // Act
        EmbeddingResult result = provider.embed(request);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getVectors(), "Vectors should not be null");
        assertEquals(1, result.getVectors().size(), "Should have one embedding");
        assertTrue(result.getVectors().get(0).length > 0, "Embedding should have dimensions");
        assertEquals("gemini", result.getProvider(), "Provider name should be gemini");
        assertEquals(768, result.getVectors().get(0).length, "Gemini free tier uses 768 dimensions");
        assertNotNull(result.getUsage(), "Usage metrics should be tracked");
        assertTrue(result.getUsage().getLatencyMs() > 0, "Latency should be positive");
    }

    @Test
    @DisplayName("Embedding dimension should be 768 for Gemini free tier")
    public void testEmbeddingDimension() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Arrange
        List<String> texts = Arrays.asList("Sample CV text");
        EmbeddingRequest request = EmbeddingRequest.builder()
                .texts(texts)
                .model("embedding-001")
                .dimensions(768)
                .build();

        // Act
        EmbeddingResult result = provider.embed(request);

        // Assert
        assertEquals(768, result.getVectors().get(0).length, 
                "Gemini embedding-001 always uses 768 dimensions");
    }

    @Test
    @DisplayName("Configuration should have Gemini free tier embedding settings")
    public void testConfigurationSettings() {
        assertNotNull(aiProperties, "AiProperties should be injected");
        assertNotNull(aiProperties.getEmbedding(), "Embedding config should exist");
    }

    @Test
    @DisplayName("Should handle zero values in embedding vector")
    public void testEmbeddingValues() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Arrange
        List<String> texts = Arrays.asList("Product Manager at StartupXYZ");
        EmbeddingRequest request = EmbeddingRequest.builder()
                .texts(texts)
                .model("embedding-001")
                .dimensions(768)
                .build();

        // Act
        EmbeddingResult result = provider.embed(request);

        // Assert
        float[] embedding = result.getVectors().get(0);
        assertTrue(embedding.length > 0);
        
        // At least some values should be non-zero
        boolean hasNonZero = false;
        for (float val : embedding) {
            if (Math.abs(val) > 0.001f) { // Allow for floating point precision
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero, "Embedding should have non-zero values");
    }

    @Test
    @DisplayName("Should work with short and long text")
    public void testVariableTextLength() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Short text
        EmbeddingRequest shortRequest = EmbeddingRequest.builder()
                .texts(Arrays.asList("CEO"))
                .model("embedding-001")
                .dimensions(768)
                .build();

        EmbeddingResult shortResult = provider.embed(shortRequest);
        assertNotNull(shortResult);
        assertEquals(768, shortResult.getVectors().get(0).length);

        // Medium text
        EmbeddingRequest mediumRequest = EmbeddingRequest.builder()
                .texts(Arrays.asList("Software Engineer with 10 years of experience in backend development"))
                .model("embedding-001")
                .dimensions(768)
                .build();

        EmbeddingResult mediumResult = provider.embed(mediumRequest);
        assertNotNull(mediumResult);
        assertEquals(768, mediumResult.getVectors().get(0).length);
    }
}
