package com.recruitment.backend.services.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.services.ai.config.AiConfigLoader;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.StructuredExtractionRequest;
import com.recruitment.backend.services.ai.model.StructuredExtractionResult;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import com.recruitment.backend.services.ai.providers.TextExtractionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
    
@Service
@Slf4j
public class CvStructuredExtractionService {

    private static final String STEP_NAME = "STRUCTURED_EXTRACTION";

    private final ProviderRegistry providerRegistry;
    private final PromptTemplateProvider promptTemplateProvider;
    private final AiConfigLoader aiConfigLoader;
    private final AiProperties aiProperties;
    private final AiRunLoggingService aiRunLoggingService;
    private final ObjectMapper objectMapper;
    private final JsonCleanerService jsonCleanerService;

    public CvStructuredExtractionService(
            ProviderRegistry providerRegistry,
            PromptTemplateProvider promptTemplateProvider,
            AiConfigLoader aiConfigLoader,
            AiProperties aiProperties,
            AiRunLoggingService aiRunLoggingService,
            ObjectMapper objectMapper,
            JsonCleanerService jsonCleanerService
    ) {
        this.providerRegistry = providerRegistry;
        this.promptTemplateProvider = promptTemplateProvider;
        this.aiConfigLoader = aiConfigLoader;
        this.aiProperties = aiProperties;
        this.aiRunLoggingService = aiRunLoggingService;
        this.objectMapper = objectMapper;
        this.jsonCleanerService = jsonCleanerService;
    }
    
        public String extract(UUID cvId, String normalizedText, String language) {
            String requestId = cvId.toString();
            String promptVersion = aiProperties.getExtraction().getPromptVersion();
            String activeVersion = promptVersion == null || promptVersion.isBlank()
                    ? aiProperties.getPrompts().getActiveVersion()
                    : promptVersion;
    
            log.debug("Starting structured extraction for CV: {} (version: {}, language: {})", 
                     cvId, activeVersion, language);
    
            // Load schema from external config
            String targetSchema = loadTargetSchema(activeVersion);
            
            // Load prompt template based on language and version
            String prompt = loadPromptTemplate(activeVersion, language, normalizedText, targetSchema);
    
            TextExtractionProvider provider = providerRegistry.getExtractionProvider();
            StructuredExtractionRequest request = StructuredExtractionRequest.builder()
                    .text(normalizedText)
                    .prompt(prompt)
                    .schema(targetSchema)
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
                     log.debug("Extraction attempt {}/{} for CV: {}", attempt, maxAttempts, cvId);
                     
                     StructuredExtractionResult result = provider.extractStructured(request);
                     String rawJson = result.getJson();
                      
                      // Log raw response info
                      log.info("Raw JSON response length: {} chars", rawJson.length());
                      if (rawJson.length() > 500) {
                          log.debug("Raw JSON (first 500 chars): {}", rawJson.substring(0, 500));
                      } else {
                          log.debug("Raw JSON: {}", rawJson);
                      }
                      
                      String cleanedJson = jsonCleanerService.cleanJson(rawJson);
                      log.info("Cleaned JSON length: {} chars", cleanedJson.length());
                      
                      String json = ensureValidJsonOrRepair(provider, request, cleanedJson, attempt, maxAttempts);
      
                      long latency = System.currentTimeMillis() - start;
                      log.info("✓ Structured extraction succeeded for CV: {} in {}ms (attempt {}/{})", 
                              cvId, latency, attempt, maxAttempts);
                      log.info("Final JSON length: {} chars", json.length());
                     
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
                    log.warn("✗ Extraction attempt {}/{} failed for CV: {} - {}", 
                            attempt, maxAttempts, cvId, ex.getMessage());
                    
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
    
                    if (attempt < maxAttempts) {
                        log.debug("Retrying extraction for CV: {} (attempt {})", cvId, attempt + 1);
                    }
                }
            }
    
            log.error("✗ Structured extraction failed for CV: {} after {} attempts", cvId, maxAttempts);
            throw new IllegalStateException(
                    String.format("Structured extraction failed for CV %s after %d attempts: %s",
                            cvId, maxAttempts, lastException != null ? lastException.getMessage() : "Unknown error"),
                    lastException
            );
        }
    
        private String ensureValidJsonOrRepair(
                 TextExtractionProvider provider,
                 StructuredExtractionRequest request,
                 String json,
                 int attempt,
                 int maxAttempts
         ) {
             if (isValidJson(json)) {
                 log.debug("JSON validation passed");
                 return json;
             }
     
             log.warn("Invalid JSON detected, attempting repair");
             log.warn("Received invalid response: {}", json);
     
             if (attempt >= maxAttempts) {
                 log.error("Invalid JSON and no more retry attempts left");
                 throw new IllegalStateException("Invalid JSON and no more retry attempts");
             }
     
             StructuredExtractionRequest repairRequest = StructuredExtractionRequest.builder()
                     .text(request.getText())
                     .prompt(request.getPrompt() + "\n\n[IMPORTANT] Your previous response was invalid JSON. "
                             + "Return ONLY valid JSON matching the schema, nothing else. No markdown, no explanations.")
                     .schema(request.getSchema())
                     .model(request.getModel())
                     .temperature(Math.min(request.getTemperature(), 0.1))  // Lower temperature for repair
                     .maxTokens(request.getMaxTokens())
                     .timeoutMs(request.getTimeoutMs())
                     .build();
     
             log.debug("Attempting JSON repair with lower temperature (0.1)");
             StructuredExtractionResult repaired = provider.extractStructured(repairRequest);
             String repairedJson = repaired.getJson();
             
             log.info("Repaired response: {}", repairedJson);
             
             if (!isValidJson(repairedJson)) {
                 log.error("Model output is still not valid JSON even after repair attempt");
                 throw new IllegalStateException("Model output is not valid JSON even after repair attempt");
             }
             
             log.info("JSON repair successful!");
             return repairedJson;
         }
    
         private boolean isValidJson(String json) {
              if (json == null || json.isBlank()) {
                  log.error("JSON is null or blank!");
                  return false;
              }
              
              String trimmed = json.trim();
              
              // Check if it looks like JSON (starts with { and ends with })
              if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                  log.error("JSON does not start with {{ or end with }}: {}",
                      trimmed.substring(0, Math.min(50, trimmed.length())));
                  return false;
              }
              
              try {
                  objectMapper.readTree(trimmed);
                  log.debug("✓ JSON validation passed");
                  return true;
              } catch (Exception e) {
                  log.error("JSON validation failed: {}", e.getMessage());
                  log.debug("Raw JSON: {}", trimmed.substring(0, Math.min(100, trimmed.length())));
                  return false;
              }
          }
    
        private String loadTargetSchema(String version) {
            // Load JSON schema from configuration
            String schemaKey = version + ".cv_profile.json";
            String schema = aiConfigLoader.getJsonSchema(schemaKey);
            
            if (schema.isBlank()) {
                log.warn("Schema not found: {}, using built-in default", schemaKey);
                schema = targetSchemaDefault();
            }
            
            return schema;
        }
    
        private String loadPromptTemplate(String version, String language, String normalizedText, String targetSchema) {
            // Prepare variables for substitution
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("cv_text", normalizedText);
            vars.put("language", language);
            vars.put("target_schema", targetSchema);
    
            // Load prompt template with language fallback to default
            try {
                return promptTemplateProvider.getPrompt("cv_extract_profile", language, version, vars);
            } catch (IllegalStateException e) {
                log.warn("Prompt not found for language: {}, trying default", language);
                try {
                    return promptTemplateProvider.getPrompt("cv_extract_profile", "default", version, vars);
                } catch (IllegalStateException fallbackError) {
                    log.warn("Prompt not found for default locale either, using minimal fallback");
                    // Minimal fallback prompt
                    return "You are a CV parser. Extract the following information as JSON:\n" + normalizedText;
                }
            }
        }
    
        private String targetSchemaDefault() {
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
