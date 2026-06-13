package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobRequirementItem;
import com.recruitment.backend.domain.entities.JobRequirementSection;
import com.recruitment.backend.domain.entities.MockInterviewQuestion;
import com.recruitment.backend.domain.entities.MockInterviewSession;
import com.recruitment.backend.domain.entities.MockInterviewTurn;
import com.recruitment.backend.services.ai.config.AiConfigLoader;
import com.recruitment.backend.services.ai.pipeline.JsonCleanerService;
import com.recruitment.backend.services.ai.providers.PromptTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockInterviewAiService {
    private static final String VERSION = "mock_interview_v1";

    private final PromptTemplateProvider promptTemplateProvider;
    private final AiConfigLoader aiConfigLoader;
    private final JsonCleanerService jsonCleanerService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.mock-interview.model:gemini-2.5-flash}")
    private String model;

    @Value("${ai.mock-interview.fallback-model:gemini-2.5-flash-lite}")
    private String fallbackModel;

    @Value("${ai.mock-interview.timeout-ms:7000}")
    private int timeoutMs;

    public JsonNode generateQuestionPlan(MockInterviewSession session, int questionCount) {
        Job job = session.getJob();
        Map<String, String> variables = baseJobVariables(job);
        variables.put("target_schema", requireSchema("question_plan"));
        variables.put("interview_type", session.getInterviewType().name());
        variables.put("question_count", String.valueOf(questionCount));
        variables.put("language", session.getLanguage());
        variables.put("session_seed", session.getId().toString());
        return call("question_plan", session.getLanguage(), variables, 0.75, 3000);
    }

    public String generateFollowUp(MockInterviewQuestion parent, String answer) {
        MockInterviewSession session = parent.getSession();
        Map<String, String> variables = baseJobVariables(session.getJob());
        variables.put("target_schema", requireSchema("follow_up"));
        variables.put("language", session.getLanguage());
        variables.put("parent_question", parent.getQuestionText());
        variables.put("competency", safe(parent.getCompetency()));
        variables.put("expected_topics", safe(parent.getExpectedTopics()));
        variables.put("candidate_answer", safe(answer));
        JsonNode node = call("follow_up", session.getLanguage(), variables, 0.65, 700);
        String question = node.path("question").asText("").trim();
        if (question.isBlank()) {
            throw new IllegalStateException("Gemini returned an empty follow-up question");
        }
        return question;
    }

    public JsonNode generateFeedback(MockInterviewSession session, List<MockInterviewTurn> turns) {
        Map<String, String> variables = baseJobVariables(session.getJob());
        variables.put("target_schema", requireSchema("feedback"));
        variables.put("language", session.getLanguage());
        variables.put("interview_type", session.getInterviewType().name());
        variables.put("questions_and_answers", buildQuestionsAndAnswers(turns));
        variables.put("rubric", """
                CONTENT_KNOWLEDGE=30, RELEVANCE=25, EVIDENCE=20, STRUCTURE=15, CLARITY=10
                """);
        return call("feedback", session.getLanguage(), variables, 0.2, 5000);
    }

    private JsonNode call(
            String task,
            String locale,
            Map<String, String> variables,
            double temperature,
            int maxTokens
    ) {
        String prompt = promptTemplateProvider.getPrompt(task, locale, VERSION, variables);
        RuntimeException primaryError;
        try {
            return callModel(model, prompt, temperature, maxTokens);
        } catch (RuntimeException exception) {
            primaryError = exception;
            log.warn("Mock interview model {} failed. Trying {} immediately: {}",
                    model, fallbackModel, exception.getMessage());
        }
        try {
            return callModel(fallbackModel, prompt, temperature, maxTokens);
        } catch (RuntimeException fallbackError) {
            fallbackError.addSuppressed(primaryError);
            throw new IllegalStateException("Both mock interview Gemini models failed", fallbackError);
        }
    }

    private JsonNode callModel(
            String targetModel,
            String prompt,
            double temperature,
            int maxTokens
    ) {
        try {
            JsonNode requestBody = objectMapper.createObjectNode()
                    .set("contents", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode()
                                    .set("parts", objectMapper.createArrayNode()
                                            .add(objectMapper.createObjectNode()
                                                    .put("text", prompt)))));
            ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                    .set("generationConfig", objectMapper.createObjectNode()
                            .put("temperature", temperature)
                            .put("maxOutputTokens", maxTokens)
                            .put("responseMimeType", "application/json"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                            + targetModel + ":generateContent?key=" + apiKey))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(requestBody)))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Gemini HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String content = responseJson.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("Gemini returned empty mock interview content");
            }
            return objectMapper.readTree(jsonCleanerService.cleanJson(content));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mock interview Gemini call was interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Mock interview Gemini call failed", exception);
        }
    }

    private String requireSchema(String task) {
        String schema = aiConfigLoader.getJsonSchema(VERSION + "." + task + ".json");
        if (schema.isBlank()) {
            throw new IllegalStateException("Mock interview schema not found for " + task);
        }
        return schema;
    }

    private Map<String, String> baseJobVariables(Job job) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("job_title", safe(job.getTitle()));
        variables.put("job_level", job.getLevel() == null ? "" : job.getLevel().name());
        variables.put("job_description", safe(job.getDescription()));
        variables.put("job_requirements", buildRequirementText(job));
        variables.put("job_location", safe(job.getLocation()));
        return variables;
    }

    private String buildRequirementText(Job job) {
        if (job.getRequirementSections() == null || job.getRequirementSections().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JobRequirementSection section : job.getRequirementSections()) {
            builder.append(section.getTitle())
                    .append(" [")
                    .append(section.getSectionType())
                    .append("]\n");
            if (section.getItems() != null) {
                for (JobRequirementItem item : section.getItems()) {
                    builder.append("- ").append(item.getContent()).append("\n");
                }
            }
        }
        return builder.toString().trim();
    }

    private String buildQuestionsAndAnswers(List<MockInterviewTurn> turns) {
        StringBuilder builder = new StringBuilder();
        for (MockInterviewTurn turn : turns) {
            String questionId = turn.getQuestion() == null
                    ? ""
                    : turn.getQuestion().getId().toString();
            builder.append(turn.getSpeaker())
                    .append(" [question_id=")
                    .append(questionId)
                    .append("]: ")
                    .append(turn.getContent())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
