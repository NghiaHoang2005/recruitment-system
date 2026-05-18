package com.recruitment.backend.notifications.services;

import com.recruitment.backend.notifications.dto.RenderedTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationTemplateRenderer {

    public RenderedTemplate render(String subjectTemplate, String bodyTemplate, Map<String, Object> payload) {
        return new RenderedTemplate(
                applyVariables(subjectTemplate, payload),
                applyVariables(bodyTemplate, payload)
        );
    }

    private String applyVariables(String template, Map<String, Object> payload) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("{{" + entry.getKey() + "}}", value);
        }
        return rendered;
    }
}
