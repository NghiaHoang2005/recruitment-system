package com.recruitment.backend.services.ai.providers.impl;

import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VersionedPromptTemplateProvider implements PromptTemplateProvider {

    private final AiProperties aiProperties;

    public VersionedPromptTemplateProvider(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public String getPrompt(String task, String locale, String version, Map<String, String> variables) {
        String key = String.join(".", version, locale, task);
        String template = aiProperties.getPrompts().getTemplates().get(key);
        if (template == null) {
            String fallbackKey = String.join(".", version, "default", task);
            template = aiProperties.getPrompts().getTemplates().get(fallbackKey);
        }

        if (template == null) {
            throw new IllegalStateException("Prompt template not found for task=" + task + ", locale=" + locale + ", version=" + version);
        }

        String resolved = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return resolved;
    }
}
