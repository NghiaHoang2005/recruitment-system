package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CvStructuredExtractionService {

    private static final String STEP_NAME = "STRUCTURED_EXTRACTION";

    private final ProviderRegistry providerRegistry;
    private final PromptTemplateProvider promptTemplateProvider;
    private final AiProperties aiProperties;
    private final AiRunLoggingService aiRunLoggingService;
    private final ObjectMapper objectMapper;

    public CvStructuredExtractionService(
            ProviderRegistry providerRegistry,
            PromptTemplateProvider promptTemplateProvider,
            AiProperties aiProperties,
            AiRunLoggingService aiRunLoggingService,
            ObjectMapper objectMapper
    ) {
        this.providerRegistry = providerRegistry;
        this.promptTemplateProvider = promptTemplateProvider;
        this.aiProperties = aiProperties;
        this.aiRunLoggingService = aiRunLoggingService;
        this.objectMapper = objectMapper;
    }

    public String extract(UUID cvId, String normalizedText, String language) {
        String requestId = cvId.toString();
        String promptVersion = aiProperties.getExtraction().getPromptVersion();
        String activeVersion = promptVersion == null || promptVersion.isBlank()
                ? aiProperties.getPrompts().getActiveVersion()
                : promptVersion;

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("cv_text", normalizedText);
        vars.put("language", language);
        vars.put("target_schema", targetSchema());

        String prompt = promptTemplateProvider.getPrompt(
                "cv_extract_profile",
                language,
                activeVersion,
                vars
        );

        TextExtractionProvider provider = providerRegistry.getExtractionProvider();
        StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                .text(normalizedText)
                .prompt(prompt)
                .schema(targetSchema())
                .model(aiProperties.getExtraction().getModel())
                .temperature(aiProperties.getExtraction().getTemperature())
                .maxTokens(aiProperties.getExtraction().getMaxTokens())
                .timeoutMs(aiProperties.getExtraction().getTimeoutMs())
                .build();

        int maxAttempts = Math.max(1, aiProperties.getExtraction().getRetries() + 1);
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                StructuredExtractionResult result = provider.extractStructured(request);
                String json = ensureValidJsonOrRepair(provider, request, result.getJson());

                long latency = System.currentTimeMillis() - start;
                aiRunLoggingService.logSuccess(
                        cvId,
                        requestId,
                        STEP_NAME,
                        result.getProvider(),
                        result.getModelName(),
                        result.getModelVersion(),
                        activeVersion,
                        result.getUsage().getInputTokens(),
                        result.getUsage().getOutputTokens(),
                        latency
                );
                return json;
            } catch (RuntimeException ex) {
                lastException = ex;
                long latency = System.currentTimeMillis() - start;
                aiRunLoggingService.logFailure(
                        cvId,
                        requestId,
                        STEP_NAME,
                        provider.providerName(),
                        request.getModel(),
                        null,
                        activeVersion,
                        ex.getMessage(),
                        latency
                );
            }
        }

        throw new IllegalStateException("Structured extraction failed", lastException);
    }

    private String ensureValidJsonOrRepair(
            TextExtractionProvider provider,
            StructuredExtractionRequest request,
            String json
    ) {
        if (isValidJson(json)) {
            return json;
        }

        StructuredExtractionRequest repairRequest = StructuredExtractionRequest.builder()
                .text(request.getText())
                .prompt(request.getPrompt() + "\n\nThe response was invalid JSON. Return only valid JSON matching schema.")
                .schema(request.getSchema())
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .timeoutMs(request.getTimeoutMs())
                .build();

        StructuredExtractionResult repaired = provider.extractStructured(repairRequest);
        if (!isValidJson(repaired.getJson())) {
            throw new IllegalStateException("Model output is not valid JSON");
        }
        return repaired.getJson();
    }

    private boolean isValidJson(String json) {
        try {
            JsonNode ignored = objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String targetSchema() {
        return "{" +
                "\"full_name\":\"string|null\"," +
                "\"email\":\"string|null\"," +
                "\"phone_number\":\"string|null\"," +
                "\"summary\":\"string|null\"," +
                "\"extracted_skills\":[\"string\"]," +
                "\"experiences\":[{" +
                "\"company\":\"string|null\"," +
                "\"title\":\"string|null\"," +
                "\"start_date\":\"string|null\"," +
                "\"end_date\":\"string|null\"," +
                "\"description\":\"string|null\"}]," +
                "\"education\":[{" +
                "\"school\":\"string|null\"," +
                "\"degree\":\"string|null\"," +
                "\"start_date\":\"string|null\"," +
                "\"end_date\":\"string|null\"}]" +
                "}";
    }
}
