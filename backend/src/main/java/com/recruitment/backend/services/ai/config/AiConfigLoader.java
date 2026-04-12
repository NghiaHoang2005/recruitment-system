package com.recruitment.backend.services.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AiConfigLoader {

    @Value("classpath:ai-prompts.yml")
    private Resource promptsResource;

    @Value("classpath:ai-schemas.yml")
    private Resource schemasResource;

    private final AiProperties aiProperties;
    private Map<String, String> cachedPrompts;
    private Map<String, Map<String, Object>> cachedSchemas;
    private Map<String, String> cachedJsonSchemas;
    private boolean loaded = false;

    public AiConfigLoader(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @PostConstruct
    public void initialize() {
        // Load prompts and inject into AiProperties
        Map<String, String> prompts = loadPrompts();
        aiProperties.getPrompts().setTemplates(prompts);
        
        // Load schemas (both full schemas and json schemas)
        loadSchemas();
        
        loaded = true;
        log.info("✓ AI Config initialization complete");
    }

    public Map<String, String> loadPrompts() {
        if (loaded && cachedPrompts != null) {
            return cachedPrompts;
        }

        try (InputStream input = promptsResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            @SuppressWarnings("unchecked")
            Map<String, String> prompts = (Map<String, String>) data.getOrDefault("prompts", new HashMap<>());

            cachedPrompts = prompts;
            log.info("✓ Loaded {} prompts from ai-prompts.yml", prompts.size());
            return prompts;
        } catch (IOException e) {
            log.error("✗ Failed to load ai-prompts.yml", e);
            return new HashMap<>();
        }
    }

    public Map<String, Map<String, Object>> loadSchemas() {
        if (loaded && cachedSchemas != null) {
            return cachedSchemas;
        }

        try (InputStream input = schemasResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> schemas = 
                (Map<String, Map<String, Object>>) data.getOrDefault("schemas", new HashMap<>());

            @SuppressWarnings("unchecked")
            Map<String, String> jsonSchemas = 
                (Map<String, String>) data.getOrDefault("json_schemas", new HashMap<>());

            cachedSchemas = schemas;
            cachedJsonSchemas = jsonSchemas;
            log.info("✓ Loaded {} schemas and {} JSON schemas from ai-schemas.yml", schemas.size(), jsonSchemas.size());
            return schemas;
        } catch (IOException e) {
            log.error("✗ Failed to load ai-schemas.yml", e);
            return new HashMap<>();
        }
    }

    public String getPrompt(String promptKey) {
        Map<String, String> prompts = loadPrompts();
        String prompt = prompts.getOrDefault(promptKey, "");
        if (prompt.isBlank()) {
            log.warn("Prompt not found: {}", promptKey);
        }
        return prompt;
    }

    public Map<String, Object> getSchema(String schemaKey) {
        Map<String, Map<String, Object>> schemas = loadSchemas();
        return schemas.getOrDefault(schemaKey, new HashMap<>());
    }

    public String getJsonSchema(String jsonSchemaKey) {
        if (cachedJsonSchemas == null) {
            loadSchemas();
        }
        String schema = cachedJsonSchemas.getOrDefault(jsonSchemaKey, "");
        if (schema.isBlank()) {
            log.warn("JSON Schema not found: {}", jsonSchemaKey);
        }
        return schema;
    }

    public void markLoaded() {
        loaded = true;
    }
}
