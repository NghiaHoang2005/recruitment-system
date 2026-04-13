package com.recruitment.backend.services.ai.providers.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@Slf4j
@ConditionalOnProperty(
    name = "ai.extraction.active-provider",
    havingValue = "gemini",
    matchIfMissing = false
)
public class GeminiTextExtractionProvider implements TextExtractionProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final RetryStrategy retryStrategy;
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/{model}:generateContent";

    public GeminiTextExtractionProvider(
            RestTemplate restTemplate,
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            RetryStrategy retryStrategy
    ) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.retryStrategy = retryStrategy;
        this.apiKey = System.getenv("GEMINI_API_KEY");
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable not set");
        }
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public StructuredExtractionResult extractStructured(StructuredExtractionRequest request) {
        long startTime = System.currentTimeMillis();
        
        String primaryModel = request.getModel();
        if (primaryModel == null || primaryModel.isBlank()) {
            primaryModel = aiProperties.getExtraction().getModel();
        }
        
        String fallbackModel = aiProperties.getExtraction().getFallbackModel();
        final String primary = primaryModel;
        final String fallback = fallbackModel;
        
        String userContent = request.getPrompt();
        Map<String, Object> requestBody = buildGeminiRequest(userContent, request);
        
        try {
            log.info("🔄 Starting CV extraction with retry strategy. Primary: {}, Fallback: {}", primary, fallback);
            log.debug("Retry config: {}", retryStrategy.getRetryConfig());
            
            // Primary operation: Call with primary model
            Supplier<StructuredExtractionResult> primaryOperation = () -> 
                callGeminiApi(primary, requestBody, startTime);
            
            // Fallback operation: Call with fallback model
            Supplier<StructuredExtractionResult> fallbackOperation = () -> {
                log.info("Attempting extraction with fallback model: {}", fallback);
                return callGeminiApi(fallback, requestBody, startTime);
            };
            
            // Execute with retry logic
            return retryStrategy.executeWithRetry(
                "CV Extraction (" + primary + ")",
                primaryOperation,
                fallbackOperation
            );
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini extraction completely failed after {}ms: {}", latency, e.getMessage(), e);
            throw new IllegalStateException("Gemini text extraction failed: " + e.getMessage(), e);
        }
    }

    private StructuredExtractionResult callGeminiApi(
            String model, 
            Map<String, Object> requestBody, 
            long startTime) {
        try {
            // Format model name with 'models/' prefix for the API
            String modelPath = model.startsWith("models/") ? model : "models/" + model;
            String apiUrl = GEMINI_API_URL.replace("{model}", modelPath) + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Gemini API with model: {}", modelPath);
            String responseJson = restTemplate.postForObject(apiUrl, entity, String.class);
            long latency = System.currentTimeMillis() - startTime;
            
            // Log raw API response for debugging
            log.debug("🔵 Gemini API raw response: {}", responseJson);

            JsonNode response = objectMapper.readTree(responseJson);
            
            if (response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("Unknown error");
                int errorCode = response.path("error").path("code").asInt(0);
                log.error("Gemini API error (code={}): {}", errorCode, errorMsg);
                
                // Treat 5xx-like errors as retriable
                if (errorCode >= 500 && errorCode < 600) {
                    throw new IllegalStateException("Gemini API 5xx error (code=" + errorCode + "): " + errorMsg);
                }
                throw new IllegalStateException("Gemini API error: " + errorMsg);
            }

            String extractedJson = response.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();
            
            // Log extracted JSON from AI model
            log.info("Gemini AI extracted JSON: {}", extractedJson);

            int inputTokens = response.path("usageMetadata").path("promptTokenCount").asInt(0);
            int outputTokens = response.path("usageMetadata").path("candidatesTokenCount").asInt(0);

            log.info("✓ Gemini extraction successful. model={}, inputTokens={}, outputTokens={}, latencyMs={}",
                    model, inputTokens, outputTokens, latency);

            return StructuredExtractionResult.builder()
                    .json(extractedJson)
                    .modelName(model)
                    .modelVersion("gemini-" + model)
                    .provider(providerName())
                    .usage(AiUsage.builder()
                            .inputTokens(inputTokens)
                            .outputTokens(outputTokens)
                            .latencyMs(latency)
                            .build())
                    .build();
                    
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini API call failed after {}ms with model {}: {}", latency, model, e.getMessage());
            throw new IllegalStateException("Gemini API call failed with model " + model + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildGeminiRequest(String userContent, StructuredExtractionRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.1);
        generationConfig.put("maxOutputTokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2000);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);
        body.put("generationConfig", generationConfig);

        // Build messages with user content containing the full prompt with context and schema
        List<Map<String, Object>> contents = Arrays.asList(
            Map.of("role", "user", "parts", Arrays.asList(
                Map.of("text", userContent)
            ))
        );
        body.put("contents", contents);

        return body;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        private List<Candidate> candidates;
        private UsageMetadata usageMetadata;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Candidate {
            private Content content;
            @JsonProperty("finish_reason")
            private String finishReason;

            @Getter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Content {
                private String role;
                private List<Part> parts;

                @Getter
                @NoArgsConstructor
                @AllArgsConstructor
                public static class Part {
                    private String text;
                }
            }
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UsageMetadata {
            @JsonProperty("prompt_token_count")
            private int promptTokenCount;

            @JsonProperty("candidates_token_count")
            private int candidatesTokenCount;

            @JsonProperty("total_token_count")
            private int totalTokenCount;
        }
    }
}
