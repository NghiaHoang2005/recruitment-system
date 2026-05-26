package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.services.ai.config.AiConfigLoader;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.JobExtractionResult;
import com.recruitment.backend.services.ai.model.JobStructuredExtractionPayload;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class JobStructuredExtractionService {

    private static final String PROMPT_VERSION = "job_extract_v1";
    private static final String PROMPT_TASK = "job_extract_profile";

    private final ProviderRegistry providerRegistry;
    private final PromptTemplateProvider promptTemplateProvider;
    private final AiConfigLoader aiConfigLoader;
    private final AiProperties aiProperties;
    private final JsonCleanerService jsonCleanerService;
    private final ObjectMapper objectMapper;

    public JobStructuredExtractionService(
            ProviderRegistry providerRegistry,
            PromptTemplateProvider promptTemplateProvider,
            AiConfigLoader aiConfigLoader,
            AiProperties aiProperties,
            JsonCleanerService jsonCleanerService,
            ObjectMapper objectMapper
    ) {
        this.providerRegistry = providerRegistry;
        this.promptTemplateProvider = promptTemplateProvider;
        this.aiConfigLoader = aiConfigLoader;
        this.aiProperties = aiProperties;
        this.jsonCleanerService = jsonCleanerService;
        this.objectMapper = objectMapper;
    }

    public JobStructuredExtractionPayload extract(Job job, String language, String requirementsText) {
        String schema = loadTargetSchema();
        String prompt = loadPromptTemplate(job, language, requirementsText, schema);

        TextExtractionProvider provider = providerRegistry.getExtractionProvider();
        StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                .text(job.getNormalizedText() == null ? job.getDescription() : job.getNormalizedText())
                .prompt(prompt)
                .schema(schema)
                .model(aiProperties.getExtraction().getModel())
                .temperature(aiProperties.getExtraction().getTemperature())
                .maxTokens(aiProperties.getExtraction().getMaxTokens())
                .timeoutMs(aiProperties.getExtraction().getTimeoutMs())
                .build();

        StructuredExtractionResult result = provider.extractStructured(request);
        String cleanedJson = jsonCleanerService.cleanJson(result.getJson());
        JobExtractionResult parsed = parseResult(cleanedJson);
        return new JobStructuredExtractionPayload(cleanedJson, parsed);
    }

    private JobExtractionResult parseResult(String json) {
        try {
            return objectMapper.readValue(json, JobExtractionResult.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Job extraction output is not valid JSON", ex);
        }
    }

    private String loadTargetSchema() {
        String schemaKey = PROMPT_VERSION + ".job_profile.json";
        String schema = aiConfigLoader.getJsonSchema(schemaKey);
        if (schema.isBlank()) {
            throw new IllegalStateException("Job extraction schema not found: " + schemaKey);
        }
        return schema;
    }

    private String loadPromptTemplate(Job job, String language, String requirementsText, String schema) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("language", language);
        vars.put("target_schema", schema);
        vars.put("job_title", job.getTitle());
        vars.put("job_description", job.getDescription());
        vars.put("job_requirements", requirementsText == null ? "" : requirementsText);
        vars.put("job_location", job.getLocation() == null ? "" : job.getLocation());

        return promptTemplateProvider.getPrompt(PROMPT_TASK, language, PROMPT_VERSION, vars);
    }
}
