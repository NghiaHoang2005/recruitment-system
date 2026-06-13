package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiLiveTokenService {
    private final RestClient.Builder restClientBuilder;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.live.model:gemini-3.1-flash-live-preview}")
    private String model;

    @Value("${ai.live.voice:Charon}")
    private String voice;

    public LiveToken createToken() {
        Instant now = Instant.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uses", 1);
        body.put("expireTime", now.plus(30, ChronoUnit.MINUTES).toString());
        body.put("newSessionExpireTime", now.plus(1, ChronoUnit.MINUTES).toString());

        JsonNode response = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1alpha/auth_tokens")
                        .queryParam("key", apiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String token = response == null ? "" : response.path("name").asText("");
        if (token.isBlank()) {
            throw new IllegalStateException("Gemini did not return an ephemeral Live API token");
        }
        return new LiveToken(token, model, voice);
    }

    public record LiveToken(String token, String model, String voice) {
    }
}
