package com.recruitment.backend.services.ai.providers;

import java.util.Map;

public interface PromptTemplateProvider {
    String getPrompt(String task, String locale, String version, Map<String, String> variables);
}
