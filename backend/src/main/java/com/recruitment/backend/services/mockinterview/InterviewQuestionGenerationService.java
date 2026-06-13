package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobRequirementItem;
import com.recruitment.backend.domain.entities.MockInterviewQuestion;
import com.recruitment.backend.domain.entities.MockInterviewSession;
import com.recruitment.backend.domain.enums.MockInterviewType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

@Service
@Slf4j
public class InterviewQuestionGenerationService {
    private static final String DEFAULT_RUBRIC = """
            {"CONTENT_KNOWLEDGE":30,"RELEVANCE":25,"EVIDENCE":20,"STRUCTURE":15,"CLARITY":10}
            """;

    private final ObjectMapper objectMapper;
    private final ObjectProvider<MockInterviewAiService> aiServiceProvider;

    public InterviewQuestionGenerationService(
            ObjectMapper objectMapper,
            ObjectProvider<MockInterviewAiService> aiServiceProvider
    ) {
        this.objectMapper = objectMapper;
        this.aiServiceProvider = aiServiceProvider;
    }

    public List<MockInterviewQuestion> createQuestionPlan(MockInterviewSession session) {
        int count = switch (session.getPlannedDurationMinutes()) {
            case 5 -> 3;
            case 15 -> 7;
            default -> 5;
        };

        MockInterviewAiService aiService =
                aiServiceProvider == null ? null : aiServiceProvider.getIfAvailable();
        if (aiService != null) {
            try {
                List<MockInterviewQuestion> aiQuestions =
                        parseAiQuestionPlan(session, aiService.generateQuestionPlan(session, count));
                if (aiQuestions.size() == count) {
                    log.info("Generated {} AI interview questions for session {}",
                            count, session.getId());
                    return aiQuestions;
                }
                log.warn("Gemini question plan returned {} questions, expected {}. Using fallback.",
                        aiQuestions.size(), count);
            } catch (RuntimeException exception) {
                log.warn("Both Gemini models failed for question plan in session {}. "
                                + "Using deterministic fallback questions: {}",
                        session.getId(), exception.getMessage(), exception);
            }
        }
        log.info("Generated {} deterministic fallback questions for session {}", count, session.getId());
        return createFallbackQuestionPlan(session, count);
    }

    private List<MockInterviewQuestion> createFallbackQuestionPlan(
            MockInterviewSession session,
            int count
    ) {
        Job job = session.getJob();
        Random random = new Random(session.getId().getMostSignificantBits()
                ^ session.getId().getLeastSignificantBits());
        List<QuestionSeed> seeds = new ArrayList<>();
        seeds.add(pick(random, List.of(
                new QuestionSeed(
                        "INTRO",
                        "Bạn hãy giới thiệu ngắn gọn về bản thân và lý do bạn quan tâm đến vị trí "
                                + job.getTitle() + ".",
                        "COMMUNICATION",
                        List.of("kinh nghiệm liên quan", "động lực", "mục tiêu")),
                new QuestionSeed(
                        "INTRO",
                        "Điều gì trong kinh nghiệm của bạn khiến bạn phù hợp với vị trí "
                                + job.getTitle() + "?",
                        "COMMUNICATION",
                        List.of("kinh nghiệm", "mức độ phù hợp", "điểm mạnh")),
                new QuestionSeed(
                        "INTRO",
                        "Nếu chỉ có hai phút để giới thiệu bản thân với nhà tuyển dụng cho vị trí "
                                + job.getTitle() + ", bạn sẽ nói những gì?",
                        "COMMUNICATION",
                        List.of("giới thiệu", "kinh nghiệm nổi bật", "động lực"))
        )));

        List<QuestionSeed> behavioralPool = List.of(
                new QuestionSeed(
                    "BEHAVIORAL",
                    "Hãy kể về một tình huống khó khăn trong công việc hoặc học tập và cách bạn đã xử lý.",
                    "PROBLEM_SOLVING",
                    List.of("tình huống", "nhiệm vụ", "hành động", "kết quả")),
                new QuestionSeed(
                    "BEHAVIORAL",
                    "Hãy chia sẻ một lần bạn phải phối hợp với người khác để đạt mục tiêu chung.",
                    "COLLABORATION",
                    List.of("vai trò", "giao tiếp", "kết quả")),
                new QuestionSeed(
                    "BEHAVIORAL",
                    "Hãy kể về một lần bạn nhận được phản hồi khó nghe. Bạn đã phản ứng và thay đổi như thế nào?",
                    "GROWTH_MINDSET",
                    List.of("phản hồi", "phản ứng", "thay đổi")),
                new QuestionSeed(
                    "BEHAVIORAL",
                    "Khi có nhiều việc quan trọng cùng lúc, bạn đã ưu tiên và quản lý tiến độ như thế nào?",
                    "PRIORITIZATION",
                    List.of("ưu tiên", "kế hoạch", "kết quả")),
                new QuestionSeed(
                    "BEHAVIORAL",
                    "Hãy chia sẻ một quyết định bạn từng đưa ra khi chưa có đầy đủ thông tin.",
                    "DECISION_MAKING",
                    List.of("thông tin", "rủi ro", "quyết định", "kết quả"))
        );

        List<QuestionSeed> technicalPool = List.of(
                new QuestionSeed(
                    "TECHNICAL",
                    "Theo bạn, kiến thức hoặc kỹ năng quan trọng nhất để làm tốt vị trí "
                            + job.getTitle() + " là gì? Hãy giải thích bằng kinh nghiệm của bạn.",
                    "TECHNICAL_KNOWLEDGE",
                    requirementTopics(job)),
                new QuestionSeed(
                    "TECHNICAL",
                    "Hãy mô tả một dự án liên quan nhất đến vị trí này, quyết định kỹ thuật của bạn và kết quả đạt được.",
                    "PRACTICAL_EXPERIENCE",
                    List.of("bối cảnh", "quyết định", "trade-off", "kết quả")),
                new QuestionSeed(
                    "TECHNICAL",
                    "Khi một hệ thống hoặc quy trình bạn phụ trách gặp sự cố, bạn thường chẩn đoán nguyên nhân theo các bước nào?",
                    "TROUBLESHOOTING",
                    List.of("quan sát", "giả thuyết", "kiểm chứng", "khắc phục")),
                new QuestionSeed(
                    "TECHNICAL",
                    "Hãy kể về một lần bạn phải lựa chọn giữa hai giải pháp kỹ thuật. Bạn đã so sánh chúng dựa trên tiêu chí nào?",
                    "TECHNICAL_DECISION",
                    List.of("phương án", "tiêu chí", "trade-off", "quyết định")),
                new QuestionSeed(
                    "TECHNICAL",
                    "Bạn làm gì để bảo đảm chất lượng khi triển khai một tính năng hoặc thay đổi quan trọng?",
                    "QUALITY",
                    List.of("kiểm thử", "review", "monitoring", "rollback"))
        );

        int middleQuestionCount = Math.max(1, count - 2);
        if (session.getInterviewType() == MockInterviewType.MIXED) {
            for (int index = 0; index < middleQuestionCount; index++) {
                List<QuestionSeed> pool = index % 2 == 0 ? technicalPool : behavioralPool;
                seeds.add(pickDifferent(random, pool, seeds));
            }
        } else if (session.getInterviewType() == MockInterviewType.TECHNICAL) {
            addDifferent(random, seeds, technicalPool, middleQuestionCount);
        } else {
            addDifferent(random, seeds, behavioralPool, middleQuestionCount);
        }

        seeds.add(pick(random, List.of(
                new QuestionSeed(
                        "REFLECTION",
                        "Nếu được bắt đầu lại một dự án gần đây, bạn sẽ thay đổi điều gì và vì sao?",
                        "SELF_REFLECTION",
                        List.of("bài học", "lý do", "cách cải thiện")),
                new QuestionSeed(
                        "REFLECTION",
                        "Bài học nghề nghiệp quan trọng nhất bạn rút ra trong thời gian gần đây là gì?",
                        "SELF_REFLECTION",
                        List.of("bài học", "bối cảnh", "cách áp dụng")),
                new QuestionSeed(
                        "CLOSING",
                        "Trong 6 đến 12 tháng tới, bạn muốn phát triển kỹ năng nào để phù hợp hơn với vị trí này?",
                        "GROWTH",
                        List.of("kỹ năng", "kế hoạch", "mục tiêu"))
        )));

        List<MockInterviewQuestion> questions = new ArrayList<>();
        for (int index = 0; index < Math.min(count, seeds.size()); index++) {
            QuestionSeed seed = seeds.get(index);
            questions.add(MockInterviewQuestion.builder()
                    .session(session)
                    .sequenceNumber(index + 1)
                    .questionType(seed.type())
                    .questionText(seed.text())
                    .competency(seed.competency())
                    .expectedTopics(writeJson(seed.expectedTopics()))
                    .rubric(DEFAULT_RUBRIC.trim())
                    .followUp(false)
                    .build());
        }
        return questions;
    }

    public String createFollowUp(MockInterviewQuestion parent, String answer) {
        MockInterviewAiService aiService =
                aiServiceProvider == null ? null : aiServiceProvider.getIfAvailable();
        if (aiService != null) {
            try {
                String followUp = aiService.generateFollowUp(parent, answer);
                log.info("Generated AI follow-up for question {}", parent.getId());
                return followUp;
            } catch (RuntimeException exception) {
                log.warn("Both Gemini models failed for follow-up question {}. "
                                + "Using contextual fallback: {}",
                        parent.getId(), exception.getMessage(), exception);
            }
        }
        String fallback = createFallbackFollowUp(parent, answer);
        log.info("Generated contextual fallback follow-up for question {}", parent.getId());
        return fallback;
    }

    private String createFallbackFollowUp(MockInterviewQuestion parent, String answer) {
        String normalized = answer == null ? "" : answer.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int wordCount = normalized.isBlank() ? 0 : normalized.split("\\s+").length;
        if (wordCount < 15) {
            return pickFollowUp(parent, normalized, List.of(
                    "Bạn có thể mô tả tình huống đó cụ thể hơn không?",
                    "Trong tình huống đó, nhiệm vụ hoặc trách nhiệm chính của bạn là gì?",
                    "Bạn có thể nói rõ hơn về hành động cụ thể mà bạn đã thực hiện không?"));
        }
        if ((lower.contains("chúng tôi") || lower.contains("nhóm") || lower.contains("team"))
                && !lower.contains("tôi đã") && !lower.contains("vai trò")) {
            return pickFollowUp(parent, normalized, List.of(
                    "Trong kết quả chung đó, phần việc và đóng góp cụ thể của riêng bạn là gì?",
                    "Bạn chịu trách nhiệm trực tiếp cho quyết định hoặc hành động nào?",
                    "Nếu tách khỏi công việc của cả nhóm, đóng góp nổi bật nhất của bạn là gì?"));
        }
        if (parent.getCompetency() != null
                && (parent.getCompetency().contains("TECHNICAL")
                || parent.getCompetency().equals("TROUBLESHOOTING")
                || parent.getCompetency().equals("QUALITY"))) {
            return pickFollowUp(parent, normalized, List.of(
                    "Bạn đã cân nhắc những phương án nào khác và vì sao không chọn chúng?",
                    "Trade-off lớn nhất của giải pháp đó là gì?",
                    "Bạn đã kiểm chứng giải pháp này hoạt động đúng bằng cách nào?",
                    "Nếu quy mô hệ thống tăng gấp mười lần, bạn sẽ thay đổi thiết kế ở điểm nào?"));
        }
        boolean mentionsOutcome = lower.contains("kết quả")
                || lower.contains("sau đó")
                || lower.contains("cuối cùng")
                || lower.contains("đạt được")
                || lower.contains("hoàn thành");
        if (!mentionsOutcome) {
            return pickFollowUp(parent, normalized, List.of(
                    "Điểm thay đổi rõ ràng nhất sau cách xử lý của bạn là gì?",
                    "Những người liên quan phản hồi thế nào sau quyết định của bạn?",
                    "Bạn dựa vào dấu hiệu nào để kết luận cách xử lý đó có hiệu quả?"));
        }
        if (!normalized.matches(".*\\d.*")) {
            return pickFollowUp(parent, normalized, List.of(
                    "Bạn dựa vào dấu hiệu hoặc bằng chứng nào để biết kết quả đó là tốt?",
                    "Nếu không có số liệu cụ thể, bạn đã nhận được phản hồi nào cho thấy giải pháp có hiệu quả?",
                    "Kết quả đó có thể được mô tả cụ thể hơn về phạm vi hoặc mức độ ảnh hưởng không?"));
        }
        return pickFollowUp(parent, normalized, List.of(
                "Quyết định khó nhất trong tình huống đó là gì?",
                "Nếu làm lại, bạn sẽ thay đổi bước nào?",
                "Bài học nào từ tình huống này có thể áp dụng cho vị trí bạn đang phỏng vấn?",
                "Rủi ro lớn nhất bạn đã phải quản lý là gì?"));
    }

    private List<MockInterviewQuestion> parseAiQuestionPlan(
            MockInterviewSession session,
            JsonNode root
    ) {
        JsonNode questionsNode = root.path("questions");
        if (!questionsNode.isArray()) {
            throw new IllegalStateException("Gemini question plan does not contain questions array");
        }
        List<MockInterviewQuestion> questions = new ArrayList<>();
        int sequence = 1;
        for (JsonNode node : questionsNode) {
            String questionText = node.path("question_text").asText("").trim();
            String questionType = node.path("question_type").asText("MIXED").trim();
            String competency = node.path("competency").asText("GENERAL").trim();
            if (questionText.isBlank()) {
                continue;
            }
            questions.add(MockInterviewQuestion.builder()
                    .session(session)
                    .sequenceNumber(sequence++)
                    .questionType(questionType)
                    .questionText(questionText)
                    .competency(competency)
                    .expectedTopics(writeJson(node.path("expected_topics")))
                    .rubric(DEFAULT_RUBRIC.trim())
                    .followUp(false)
                    .build());
        }
        return questions;
    }

    private QuestionSeed pick(Random random, List<QuestionSeed> pool) {
        return pool.get(random.nextInt(pool.size()));
    }

    private QuestionSeed pickDifferent(Random random, List<QuestionSeed> pool, List<QuestionSeed> selected) {
        List<QuestionSeed> available = pool.stream()
                .filter(candidate -> selected.stream().noneMatch(existing -> existing.text().equals(candidate.text())))
                .toList();
        return pick(random, available.isEmpty() ? pool : available);
    }

    private void addDifferent(Random random, List<QuestionSeed> target, List<QuestionSeed> pool, int count) {
        for (int index = 0; index < Math.min(count, pool.size()); index++) {
            target.add(pickDifferent(random, pool, target));
        }
    }

    private String pickFollowUp(MockInterviewQuestion parent, String answer, List<String> options) {
        int index = Math.floorMod(Objects.hash(parent.getId(), answer), options.size());
        return options.get(index);
    }

    private List<String> requirementTopics(Job job) {
        List<String> topics = job.getRequirementSections().stream()
                .flatMap(section -> section.getItems().stream())
                .map(JobRequirementItem::getContent)
                .filter(value -> value != null && !value.isBlank())
                .limit(5)
                .toList();
        return topics.isEmpty() ? List.of(job.getTitle(), "kiến thức chuyên môn") : topics;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private record QuestionSeed(String type, String text, String competency, List<String> expectedTopics) {
    }
}
