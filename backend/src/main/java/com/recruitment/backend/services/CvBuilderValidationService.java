package com.recruitment.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderValidationIssue;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderValidationResult;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderValidationSeverity;
import com.recruitment.backend.domain.dtos.CvBuilder.CvBuilderValidationStatus;
import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderTemplate;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CvBuilderValidationService {

    private final ObjectMapper objectMapper;

    public CvBuilderValidationResult validateSoft(String contentJson, CvBuilderTemplate template) {
        return validate(contentJson, template, false);
    }

    public CvBuilderValidationResult validateStrict(String contentJson, CvBuilderTemplate template) {
        CvBuilderValidationResult result = validate(contentJson, template, true);
        if (result.getStatus() == CvBuilderValidationStatus.HAS_ERRORS) {
            throw new AppException(ErrorCode.CV_BUILDER_STRICT_VALIDATION_FAILED);
        }
        return result;
    }

    private CvBuilderValidationResult validate(String contentJson, CvBuilderTemplate template, boolean strictMode) {
        List<CvBuilderValidationIssue> issues = new ArrayList<>();
        JsonNode root = parseRoot(contentJson, issues);

        JsonNode templateSchema = readTemplateLayoutSchema(template);
        Set<String> allowedTypes = readAllowedTypes(templateSchema);
        Set<String> allowedSlots = readAllowedSlots(templateSchema);
        JsonNode sectionDefinitions = templateSchema.path("sectionDefinitions");

        if (!root.path("meta").isObject()) {
            issues.add(issue("required_missing", severity(false, strictMode), null, "meta", "Meta data is missing", "Meta is auto-generated from template."));
        }

        JsonNode sectionsNode = root.path("sections");
        if (!sectionsNode.isArray()) {
            issues.add(issue("invalid_type", CvBuilderValidationSeverity.ERROR, null, "sections", "Sections must be an array", "Ensure contentJson.sections is an array."));
            return buildResult(issues);
        }

        Set<String> seenSectionIds = new HashSet<>();
        for (int i = 0; i < sectionsNode.size(); i++) {
            JsonNode section = sectionsNode.get(i);
            String sectionPath = "sections[" + i + "]";
            if (!section.isObject()) {
                issues.add(issue("invalid_type", CvBuilderValidationSeverity.ERROR, null, sectionPath, "Section must be an object", "Provide a section object with sectionId/type/title/data."));
                continue;
            }

            String sectionId = section.path("sectionId").asText("");
            if (sectionId.isBlank()) {
                issues.add(issue("required_missing", CvBuilderValidationSeverity.ERROR, null, sectionPath + ".sectionId", "sectionId is required", "Provide unique sectionId for each section."));
            } else if (!seenSectionIds.add(sectionId)) {
                issues.add(issue("invalid_format", CvBuilderValidationSeverity.ERROR, sectionId, sectionPath + ".sectionId", "sectionId must be unique", "Generate a different sectionId."));
            }

            String type = section.path("type").asText("").trim().toLowerCase();
            if (type.isBlank()) {
                issues.add(issue("required_missing", CvBuilderValidationSeverity.ERROR, sectionIdOrNull(sectionId), sectionPath + ".type", "Section type is required", "Set a valid section type."));
                continue;
            }

            if (!allowedTypes.isEmpty() && !allowedTypes.contains(type)) {
                issues.add(issue("invalid_format", CvBuilderValidationSeverity.ERROR, sectionIdOrNull(sectionId), sectionPath + ".type", "Section type is not allowed by template", "Use allowed section types for this template."));
            }

            String slot = section.path("slot").asText("");
            if (!slot.isBlank() && !allowedSlots.isEmpty() && !allowedSlots.contains(slot)) {
                issues.add(issue("invalid_format", CvBuilderValidationSeverity.ERROR, sectionIdOrNull(sectionId), sectionPath + ".slot", "Slot is not allowed by template", "Use one of template layout slots."));
            }

            JsonNode title = section.path("title");
            if (!title.isTextual() || title.asText().isBlank()) {
                issues.add(issue("required_missing", severity(false, strictMode), sectionIdOrNull(sectionId), sectionPath + ".title", "Section title should not be empty", "Set a readable title for this section."));
            }

            JsonNode data = section.get("data");
            if (data == null || data.isNull()) {
                issues.add(issue("required_missing", CvBuilderValidationSeverity.ERROR, sectionIdOrNull(sectionId), sectionPath + ".data", "Section data is required", "Provide data object/array according to section schema."));
                continue;
            }

            JsonNode sectionSchema = sectionDefinitions.path(type).path("dataSchema");
            if (!sectionSchema.isMissingNode() && sectionSchema.isObject()) {
                validateBySchema(data, sectionSchema, sectionIdOrNull(sectionId), sectionPath + ".data", issues, strictMode);
            }
        }

        return buildResult(issues);
    }

    private JsonNode parseRoot(String contentJson, List<CvBuilderValidationIssue> issues) {
        if (contentJson == null || contentJson.isBlank()) {
            issues.add(issue("required_missing", CvBuilderValidationSeverity.ERROR, null, "contentJson", "contentJson is required", "Send non-empty JSON content."));
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode root = objectMapper.readTree(contentJson);
            if (!root.isObject()) {
                issues.add(issue("invalid_type", CvBuilderValidationSeverity.ERROR, null, "contentJson", "contentJson must be a JSON object", "Wrap data in an object with sections/meta."));
                return objectMapper.createObjectNode();
            }
            return root;
        } catch (JsonProcessingException e) {
            issues.add(issue("invalid_format", CvBuilderValidationSeverity.ERROR, null, "contentJson", "contentJson is invalid JSON", "Fix JSON format before saving."));
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode readTemplateLayoutSchema(CvBuilderTemplate template) {
        if (template == null || template.getLayoutSchema() == null || template.getLayoutSchema().isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(template.getLayoutSchema());
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private Set<String> readAllowedTypes(JsonNode schema) {
        Set<String> allowed = new HashSet<>();
        JsonNode node = schema.path("allowedSectionTypes");
        if (!node.isArray()) {
            return allowed;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                allowed.add(item.asText().trim().toLowerCase());
            }
        }
        return allowed;
    }

    private Set<String> readAllowedSlots(JsonNode schema) {
        Set<String> allowed = new HashSet<>();
        JsonNode node = schema.path("layoutRules").path("slots");
        if (!node.isArray()) {
            return allowed;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                allowed.add(item.asText());
            }
        }
        return allowed;
    }

    private void validateBySchema(
            JsonNode value,
            JsonNode schema,
            String sectionId,
            String fieldPath,
            List<CvBuilderValidationIssue> issues,
            boolean strictMode
    ) {
        String expectedType = schema.path("type").asText("");
        if (!expectedType.isBlank() && !isTypeMatch(value, expectedType)) {
            issues.add(issue("invalid_type", CvBuilderValidationSeverity.ERROR, sectionId, fieldPath, "Type mismatch. Expected: " + expectedType, "Adjust value type to match schema."));
            return;
        }

        if ("string".equals(expectedType)) {
            int maxLength = schema.path("maxLength").asInt(-1);
            if (maxLength > -1 && value.isTextual() && value.asText().length() > maxLength) {
                issues.add(issue("max_length_exceeded", CvBuilderValidationSeverity.ERROR, sectionId, fieldPath, "String exceeds max length " + maxLength, "Shorten this text value."));
            }
            return;
        }

        if ("array".equals(expectedType) && value.isArray()) {
            int maxItems = schema.path("maxItems").asInt(-1);
            if (maxItems > -1 && value.size() > maxItems) {
                issues.add(issue("max_length_exceeded", CvBuilderValidationSeverity.ERROR, sectionId, fieldPath, "Array exceeds max items " + maxItems, "Remove extra items."));
            }

            JsonNode itemSchema = schema.path("items");
            if (itemSchema.isObject()) {
                for (int i = 0; i < value.size(); i++) {
                    validateBySchema(value.get(i), itemSchema, sectionId, fieldPath + "[" + i + "]", issues, strictMode);
                }
            }
            return;
        }

        if ("object".equals(expectedType) && value.isObject()) {
            Set<String> requiredFields = new HashSet<>();
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode f : required) {
                    if (f.isTextual() && !f.asText().isBlank()) {
                        requiredFields.add(f.asText());
                    }
                }
            }

            JsonNode properties = schema.path("properties");
            boolean additionalProperties = schema.path("additionalProperties").asBoolean(true);

            for (String requiredField : requiredFields) {
                if (!value.has(requiredField) || value.path(requiredField).isNull()
                        || (value.path(requiredField).isTextual() && value.path(requiredField).asText().isBlank())) {
                    issues.add(issue("required_missing", CvBuilderValidationSeverity.ERROR, sectionId, fieldPath + "." + requiredField, "Required field is missing", "Provide value for this field."));
                }
            }

            value.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = value.get(fieldName);
                JsonNode propertySchema = properties.path(fieldName);

                if (propertySchema.isMissingNode()) {
                    if (!additionalProperties) {
                        issues.add(issue(
                                "unknown_field",
                                severity(false, strictMode),
                                sectionId,
                                fieldPath + "." + fieldName,
                                "Field is not defined in schema",
                                "Remove field or move it to custom section."
                        ));
                    }
                    return;
                }

                validateBySchema(fieldValue, propertySchema, sectionId, fieldPath + "." + fieldName, issues, strictMode);
            });
        }
    }

    private boolean isTypeMatch(JsonNode value, String expectedType) {
        return switch (expectedType) {
            case "string" -> value.isTextual();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> true;
        };
    }

    private CvBuilderValidationResult buildResult(List<CvBuilderValidationIssue> issues) {
        boolean hasErrors = issues.stream().anyMatch(i -> i.getSeverity() == CvBuilderValidationSeverity.ERROR);
        boolean hasWarnings = issues.stream().anyMatch(i -> i.getSeverity() == CvBuilderValidationSeverity.WARNING);

        CvBuilderValidationStatus status;
        if (hasErrors) {
            status = CvBuilderValidationStatus.HAS_ERRORS;
        } else if (hasWarnings) {
            status = CvBuilderValidationStatus.HAS_WARNINGS;
        } else {
            status = CvBuilderValidationStatus.VALID;
        }

        return CvBuilderValidationResult.builder()
                .status(status)
                .issues(issues)
                .validatedAt(LocalDateTime.now())
                .build();
    }

    private CvBuilderValidationIssue issue(
            String code,
            CvBuilderValidationSeverity severity,
            String sectionId,
            String fieldPath,
            String message,
            String hint
    ) {
        return CvBuilderValidationIssue.builder()
                .code(code)
                .severity(severity)
                .sectionId(sectionId)
                .fieldPath(fieldPath)
                .message(message)
                .hint(hint)
                .build();
    }

    private CvBuilderValidationSeverity severity(boolean asError, boolean strictMode) {
        if (asError || strictMode) {
            return CvBuilderValidationSeverity.ERROR;
        }
        return CvBuilderValidationSeverity.WARNING;
    }

    private String sectionIdOrNull(String sectionId) {
        return (sectionId == null || sectionId.isBlank()) ? null : sectionId;
    }
}
