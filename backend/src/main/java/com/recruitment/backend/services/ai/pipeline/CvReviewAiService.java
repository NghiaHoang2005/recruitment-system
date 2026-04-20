package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.services.ai.config.AiConfigLoader;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CvReviewAiService {

    private static final String STEP_NAME = "CV_REVIEW";

    private final ProviderRegistry providerRegistry;
    private final PromptTemplateProvider promptTemplateProvider;
    private final AiConfigLoader aiConfigLoader;
    private final AiProperties aiProperties;
    private final AiRunLoggingService aiRunLoggingService;
    private final JsonCleanerService jsonCleanerService;
    private final ObjectMapper objectMapper;

    public CvReviewAiService(
            ProviderRegistry providerRegistry,
            PromptTemplateProvider promptTemplateProvider,
            AiConfigLoader aiConfigLoader,
            AiProperties aiProperties,
            AiRunLoggingService aiRunLoggingService,
            JsonCleanerService jsonCleanerService,
            ObjectMapper objectMapper
    ) {
        this.providerRegistry = providerRegistry;
        this.promptTemplateProvider = promptTemplateProvider;
        this.aiConfigLoader = aiConfigLoader;
        this.aiProperties = aiProperties;
        this.aiRunLoggingService = aiRunLoggingService;
        this.jsonCleanerService = jsonCleanerService;
        this.objectMapper = objectMapper;
    }

    public ReviewAiResult review(UUID cvId, Cv cv, Job job, String language) {
        String requestId = cvId + "-review-" + System.currentTimeMillis();
        String promptVersion = aiProperties.getExtraction().getPromptVersion();
        String activeVersion = (promptVersion == null || promptVersion.isBlank())
                ? aiProperties.getPrompts().getActiveVersion()
                : promptVersion;

        String schema = aiConfigLoader.getJsonSchema(activeVersion + ".cv_review.json");
        if (schema.isBlank()) {
            schema = reviewSchemaFallback();
        }

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("language", language == null ? "default" : language);
        vars.put("target_schema", schema);
        vars.put("cv_raw_text", cv.getRawText() == null ? "" : cv.getRawText());
        vars.put("cv_parsed_json", cv.getParsedData() == null ? "{}" : cv.getParsedData());
        vars.put("job_title", job == null ? "" : safe(job.getTitle()));
        vars.put("job_description", job == null ? "" : safe(job.getDescription()));
        vars.put("job_requirements", job == null ? "" : safe(job.getRequirements()));
        vars.put("job_location", job == null ? "" : safe(job.getLocation()));

        String task = job == null ? "cv_review_general" : "cv_review_job_match";
        String prompt = promptTemplateProvider.getPrompt(task, language, activeVersion, vars);

        TextExtractionProvider provider = providerRegistry.getExtractionProvider();
        StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                .text(cv.getRawText())
                .prompt(prompt)
                .schema(schema)
                .model(aiProperties.getExtraction().getModel())
                .temperature(aiProperties.getExtraction().getTemperature())
                .maxTokens(aiProperties.getExtraction().getMaxTokens())
                .timeoutMs(aiProperties.getExtraction().getTimeoutMs())
                .build();

        long start = System.currentTimeMillis();
        try {
            StructuredExtractionResult result = provider.extractStructured(request);
            String cleaned = jsonCleanerService.cleanJson(result.getJson());

            JsonNode jsonNode;
            try {
                jsonNode = objectMapper.readTree(cleaned);
            } catch (Exception e) {
                throw new IllegalStateException("Invalid review JSON from model");
            }

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

            return ReviewAiResult.builder()
                    .rawJson(cleaned)
                    .node(jsonNode)
                    .provider(result.getProvider())
                    .modelName(result.getModelName())
                    .modelVersion(result.getModelVersion())
                    .promptVersion(activeVersion)
                    .build();
        } catch (RuntimeException ex) {
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
            throw ex;
        }
    }

    private String safe(String input) {
        return input == null ? "" : input;
    }

    private String reviewSchemaFallback() {
        return "{" +
                "\"summary\":\"string\"," +
                "\"fit_score\":\"number|null\"," +
                "\"strengths\":[\"string\"]," +
                "\"weaknesses\":[\"string\"]," +
                "\"improvements\":[\"string\"]," +
                "\"matched_requirements\":[\"string\"]," +
                "\"missing_requirements\":[\"string\"]," +
                "\"action_plan\":[\"string\"]" +
                "}";
    }

    @Getter
    @Builder
    public static class ReviewAiResult {
        private String rawJson;
        private JsonNode node;
        private String provider;
        private String modelName;
        private String modelVersion;
        private String promptVersion;
    }
}
