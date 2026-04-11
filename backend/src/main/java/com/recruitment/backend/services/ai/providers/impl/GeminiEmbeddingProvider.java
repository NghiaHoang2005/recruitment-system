package com.recruitment.backend.services.ai.providers.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(
    name = "ai.embedding.active-provider",
    havingValue = "gemini",
    matchIfMissing = false
)
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private static final String GEMINI_EMBEDDING_API_URL = 
            "https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent";

    public GeminiEmbeddingProvider(
            RestTemplate restTemplate,
            AiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
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
    public EmbeddingResult embed(EmbeddingRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> requestBody = buildGeminiEmbeddingRequest(request);
            String apiUrl = GEMINI_EMBEDDING_API_URL + "?key=" + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String responseJson = restTemplate.postForObject(apiUrl, entity, String.class);
            long latency = System.currentTimeMillis() - startTime;

            JsonNode response = objectMapper.readTree(responseJson);
            
            if (response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("Unknown error");
                log.error("Gemini embedding API error: {}", errorMsg);
                throw new IllegalStateException("Gemini embedding API error: " + errorMsg);
            }

            JsonNode embeddingNode = response.path("embedding").path("values");
            float[] vector = jsonArrayToFloatArray(embeddingNode);

            int totalTokens = response.path("usageMetadata").path("billableCharacterCount").asInt(0);

            log.info("Gemini embedding successful. texts={}, totalTokens={}, latencyMs={}",
                    request.getTexts().size(), totalTokens, latency);

            List<float[]> vectors = new ArrayList<>();
            vectors.add(vector);

            return EmbeddingResult.builder()
                    .vectors(vectors)
                    .modelName("gemini-embedding-001")
                    .modelVersion("gemini-embedding-001")
                    .provider(providerName())
                    .dimensions(vector.length)
                    .usage(AiUsage.builder()
                            .inputTokens(totalTokens)
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

    private Map<String, Object> buildGeminiEmbeddingRequest(EmbeddingRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        if (request.getTexts() == null || request.getTexts().isEmpty()) {
            throw new IllegalArgumentException("At least one text must be provided for embedding");
        }

        String textToEmbed = request.getTexts().get(0);
        if (request.getTexts().size() > 1) {
            log.warn("Gemini embedding-001 processes one text at a time. " +
                    "Embedding first text only. {} additional texts will be ignored.", 
                    request.getTexts().size() - 1);
        }

        body.put("content", Map.of("parts", List.of(
            Map.of("text", textToEmbed)
        )));

        return body;
    }

    private float[] jsonArrayToFloatArray(JsonNode node) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("Expected array node for embedding");
        }
        float[] result = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = (float) node.get(i).asDouble();
        }
        return result;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiEmbeddingResponse {
        private Embedding embedding;
        private UsageMetadata usageMetadata;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Embedding {
            private List<Double> values;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UsageMetadata {
            @JsonProperty("prompt_token_count")
            private int promptTokenCount;

            @JsonProperty("total_token_count")
            private int totalTokenCount;

            @JsonProperty("billable_character_count")
            private int billableCharacterCount;
        }
    }
}
