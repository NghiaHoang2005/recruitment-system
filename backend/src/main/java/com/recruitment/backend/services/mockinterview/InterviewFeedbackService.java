package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.MockInterviewFeedback;
import com.recruitment.backend.domain.entities.MockInterviewSession;
import com.recruitment.backend.domain.entities.MockInterviewTurn;
import com.recruitment.backend.domain.enums.InterviewSpeaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class InterviewFeedbackService {
    private final ObjectMapper objectMapper;
    private final ObjectProvider<MockInterviewAiService> aiServiceProvider;

    public InterviewFeedbackService(
            ObjectMapper objectMapper,
            ObjectProvider<MockInterviewAiService> aiServiceProvider
    ) {
        this.objectMapper = objectMapper;
        this.aiServiceProvider = aiServiceProvider;
    }

    public MockInterviewFeedback generate(MockInterviewSession session, List<MockInterviewTurn> turns) {
        MockInterviewAiService aiService =
                aiServiceProvider == null ? null : aiServiceProvider.getIfAvailable();
        if (aiService != null) {
            try {
                return parseAiFeedback(session, aiService.generateFeedback(session, turns));
            } catch (RuntimeException exception) {
                log.warn("Gemini feedback generation failed for session {}. Using fallback: {}",
                        session.getId(), exception.getMessage());
            }
        }
        return generateFallback(session, turns);
    }

    private MockInterviewFeedback generateFallback(
            MockInterviewSession session,
            List<MockInterviewTurn> turns
    ) {
        List<MockInterviewTurn> answers = turns.stream()
                .filter(turn -> turn.getSpeaker() == InterviewSpeaker.CANDIDATE)
                .filter(MockInterviewTurn::isFinalTurn)
                .toList();

        int averageWords = answers.isEmpty() ? 0 : (int) answers.stream()
                .mapToInt(turn -> wordCount(turn.getContent()))
                .average()
                .orElse(0);
        long answersWithEvidence = answers.stream()
                .filter(turn -> containsEvidence(turn.getContent()))
                .count();

        int content = clamp(35 + averageWords, 35, 90);
        int relevance = answers.isEmpty() ? 20 : clamp(50 + answers.size() * 6, 50, 90);
        int evidence = answers.isEmpty() ? 20 : clamp(35 + (int) (answersWithEvidence * 55 / answers.size()), 35, 90);
        int structure = averageWords >= 45 ? 78 : averageWords >= 20 ? 65 : 45;
        int clarity = averageWords > 180 ? 60 : averageWords >= 20 ? 78 : 55;
        int overall = Math.round(content * .30f + relevance * .25f + evidence * .20f
                + structure * .15f + clarity * .10f);

        List<Map<String, Object>> criteria = List.of(
                criterion("CONTENT_KNOWLEDGE", content, 30, "Đánh giá dựa trên mức độ chi tiết của các câu trả lời."),
                criterion("RELEVANCE", relevance, 25, "Đánh giá dựa trên số câu hỏi đã được trả lời đầy đủ."),
                criterion("EVIDENCE", evidence, 20, "Đánh giá dựa trên ví dụ, kết quả và số liệu cụ thể."),
                criterion("STRUCTURE", structure, 15, "Đánh giá cấu trúc bối cảnh, hành động và kết quả."),
                criterion("CLARITY", clarity, 10, "Đánh giá độ rõ ràng và súc tích của transcript.")
        );

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (content >= 70) strengths.add("Câu trả lời có nội dung tương đối đầy đủ.");
        else improvements.add("Mở rộng phần hành động cụ thể và lý do đưa ra quyết định.");
        if (evidence >= 65) strengths.add("Đã sử dụng ví dụ hoặc kết quả cụ thể.");
        else improvements.add("Bổ sung con số, kết quả hoặc bằng chứng để tăng sức thuyết phục.");
        if (structure >= 70) strengths.add("Cách trình bày có cấu trúc, dễ theo dõi.");
        else improvements.add("Thử áp dụng STAR: Situation, Task, Action, Result.");
        if (strengths.isEmpty()) strengths.add("Bạn đã hoàn thành buổi luyện tập và có đủ dữ liệu để cải thiện.");
        if (improvements.isEmpty()) improvements.add("Rút gọn các chi tiết phụ để câu trả lời súc tích hơn.");

        List<Map<String, Object>> perQuestion = answers.stream()
                .map(answer -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("questionId", answer.getQuestion() == null ? null : answer.getQuestion().getId());
                    item.put("answerSummary", summarize(answer.getContent()));
                    item.put("whatWentWell", containsEvidence(answer.getContent())
                            ? List.of("Có đưa ra chi tiết hoặc bằng chứng cụ thể.")
                            : List.of("Đã trả lời trực tiếp vào câu hỏi."));
                    item.put("whatToImprove", wordCount(answer.getContent()) < 25
                            ? List.of("Câu trả lời còn ngắn; cần nói rõ hành động và kết quả.")
                            : List.of("Làm nổi bật hơn đóng góp cá nhân và bài học rút ra."));
                    item.put("betterAnswerExample",
                            "Trình bày bối cảnh ngắn gọn, nêu rõ nhiệm vụ, hành động của riêng bạn và kết quả đo lường được.");
                    return item;
                })
                .toList();

        return MockInterviewFeedback.builder()
                .session(session)
                .overallScore(overall)
                .scoreLabel(label(overall))
                .confidence(answers.size() >= 4 ? "MEDIUM" : "LOW")
                .overallSummary("Điểm luyện tập ước tính là " + overall
                        + "/100. Hãy tập trung vào bằng chứng cụ thể và cấu trúc câu trả lời.")
                .criteriaScores(writeJson(criteria))
                .strengths(writeJson(strengths))
                .improvements(writeJson(improvements))
                .nextSteps(writeJson(List.of(
                        "Luyện lại các câu trả lời theo mô hình STAR.",
                        "Thêm ít nhất một kết quả có thể đo lường cho mỗi ví dụ.",
                        "Thử trả lời mỗi câu trong 60 đến 120 giây.")))
                .questionFeedback(writeJson(perQuestion))
                .schemaVersion("mock_interview_feedback_v1")
                .build();
    }

    private MockInterviewFeedback parseAiFeedback(MockInterviewSession session, JsonNode node) {
        int overallScore = clamp(node.path("overall_score").asInt(-1), 0, 100);
        if (node.path("overall_score").isMissingNode() || overallScore < 0) {
            throw new IllegalStateException("Gemini feedback is missing overall_score");
        }
        JsonNode criteria = requireArray(node, "criteria_scores");
        JsonNode strengths = requireArray(node, "strengths");
        JsonNode improvements = requireArray(node, "improvements");
        JsonNode nextSteps = requireArray(node, "next_steps");
        JsonNode questionFeedback = requireArray(node, "question_feedback");
        String summary = node.path("overall_summary").asText("").trim();
        if (summary.isBlank() || questionFeedback.isEmpty()) {
            throw new IllegalStateException("Gemini feedback is incomplete");
        }

        return MockInterviewFeedback.builder()
                .session(session)
                .overallScore(overallScore)
                .scoreLabel(validScoreLabel(node.path("score_label").asText(""), overallScore))
                .confidence(validConfidence(node.path("confidence").asText("")))
                .overallSummary(summary)
                .criteriaScores(writeJson(criteria))
                .strengths(writeJson(strengths))
                .improvements(writeJson(improvements))
                .nextSteps(writeJson(nextSteps))
                .questionFeedback(writeJson(questionFeedback))
                .schemaVersion("mock_interview_feedback_v1")
                .build();
    }

    private JsonNode requireArray(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            throw new IllegalStateException("Gemini feedback field is not an array: " + field);
        }
        return value;
    }

    private String validScoreLabel(String value, int score) {
        return switch (value) {
            case "CAN_CAI_THIEN", "DAT", "KHA", "TOT" -> value;
            default -> label(score);
        };
    }

    private String validConfidence(String value) {
        return switch (value) {
            case "LOW", "MEDIUM", "HIGH" -> value;
            default -> "LOW";
        };
    }

    private Map<String, Object> criterion(String name, int score, int weight, String evidence) {
        return Map.of("criterion", name, "score", score, "weight", weight, "evidence", List.of(evidence));
    }

    private int wordCount(String value) {
        if (value == null || value.isBlank()) return 0;
        return value.trim().split("\\s+").length;
    }

    private boolean containsEvidence(String value) {
        if (value == null) return false;
        String text = value.toLowerCase(Locale.ROOT);
        return text.matches(".*\\d.*")
                || text.contains("kết quả")
                || text.contains("cải thiện")
                || text.contains("giảm ")
                || text.contains("tăng ");
    }

    private String summarize(String value) {
        if (value == null) return "";
        return value.length() <= 180 ? value : value.substring(0, 177) + "...";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String label(int score) {
        if (score >= 85) return "TOT";
        if (score >= 70) return "KHA";
        if (score >= 55) return "DAT";
        return "CAN_CAI_THIEN";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
