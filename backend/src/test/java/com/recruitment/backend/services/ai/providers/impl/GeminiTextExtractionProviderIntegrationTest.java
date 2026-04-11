package com.recruitment.backend.services.ai.providers.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Gemini Text Extraction Provider (FREE TIER)
 * 
 * Note: These tests require:
 * 1. GEMINI_API_KEY environment variable to be set (get free at https://aistudio.google.com/app/apikey)
 * 2. AI_EXTRACTION_PROVIDER=gemini configuration
 * 
 * These tests will be SKIPPED if GEMINI_API_KEY is not set.
 * Gemini free tier: 60 requests per minute, no credit card required
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@DisplayName("Gemini Text Extraction Provider Integration Tests")
public class GeminiTextExtractionProviderIntegrationTest {

    @Autowired(required = false)
    private GeminiTextExtractionProvider provider;

    @Autowired
    private AiProperties aiProperties;

    @Test
    @DisplayName("Provider should be configured when GEMINI_API_KEY is set")
    public void testProviderAvailable() {
        assertNotNull(provider, "Gemini text extraction provider should be available when GEMINI_API_KEY is set");
        assertEquals("gemini", provider.providerName());
    }

    @Test
    @DisplayName("Should extract structured data from CV text successfully using Gemini")
    public void testExtractStructured() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Arrange
        String cvText = "John Doe, Senior Software Engineer at TechCorp (2020-2024). " +
                       "Skills: Java, Spring Boot, Microservices. " +
                       "Education: BS Computer Science, State University 2020.";
        
        String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}," +
                       "\"title\":{\"type\":\"string\"},\"company\":{\"type\":\"string\"}," +
                       "\"skills\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}";

        StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                .text(cvText)
                .schema(schema)
                .model("gemini-1.5-flash")
                .temperature(0.1)
                .maxTokens(500)
                .build();

        // Act
        StructuredExtractionResult result = provider.extractStructured(request);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getJson(), "JSON extraction should not be null");
        assertFalse(result.getJson().isEmpty(), "JSON extraction should not be empty");
        assertEquals("gemini", result.getProvider(), "Provider name should be gemini");
        assertNotNull(result.getUsage(), "Usage metrics should be tracked");
        assertTrue(result.getUsage().getLatencyMs() > 0, "Latency should be positive");
    }

    @Test
    @DisplayName("Configuration should have Gemini free tier settings")
    public void testConfigurationSettings() {
        assertNotNull(aiProperties, "AiProperties should be injected");
        assertNotNull(aiProperties.getExtraction(), "Extraction config should exist");
    }

    @Test
    @DisplayName("Should handle different Gemini models")
    public void testDifferentModels() {
        if (provider == null) {
            return; // Skip if provider not available
        }

        // Test with gemini-1.5-pro (free tier)
        String cvText = "Jane Smith, Product Manager at StartupXYZ";
        String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";

        StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                .text(cvText)
                .schema(schema)
                .model("gemini-1.5-pro")
                .temperature(0.1)
                .maxTokens(300)
                .build();

        // Act
        StructuredExtractionResult result = provider.extractStructured(request);

        // Assert
        assertNotNull(result, "Should work with gemini-1.5-pro model");
        assertEquals("gemini", result.getProvider());
        assertTrue(result.getModelName().contains("gemini"));
    }
}
