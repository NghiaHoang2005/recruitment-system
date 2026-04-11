package com.recruitment.backend.services.ai.providers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recruitment.backend.services.ai.model.AiUsage;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import org.springframework.stereotype.Component;

@Component
public class MockTextExtractionProvider implements TextExtractionProvider {

    private final ObjectMapper objectMapper;

    public MockTextExtractionProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public StructuredExtractionResult extractStructured(StructuredExtractionRequest request) {
        String fullName = "Unknown";
        String email = null;
        String phone = null;

        for (String line : request.getText().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (fullName.equals("Unknown") && !trimmed.contains("@")) {
                fullName = trimmed;
            }
            if (email == null && trimmed.contains("@")) {
                email = trimmed;
            }
            if (phone == null && trimmed.matches(".*\\d{8,}.*")) {
                phone = trimmed;
            }
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();

            root.put("full_name", fullName);
            root.put("email", email);
            root.put("phone_number", phone);
            root.put("summary", "");

            root.set("extracted_skills", objectMapper.createArrayNode());
            root.set("experiences", objectMapper.createArrayNode());
            root.set("education", objectMapper.createArrayNode());

            String json = objectMapper.writeValueAsString(root);
            return StructuredExtractionResult.builder()
                    .json(json)
                    .modelName(request.getModel())
                    .modelVersion("mock-v1")
                    .provider(providerName())
                    .usage(AiUsage.builder()
                            .inputTokens(Math.max(1, request.getText().length() / 4))
                            .outputTokens(Math.max(1, json.length() / 4))
                            .latencyMs(10)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create mock extraction JSON", e);
        }
    }
}
