package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.MockInterviewQuestion;
import com.recruitment.backend.domain.entities.MockInterviewSession;
import com.recruitment.backend.domain.enums.MockInterviewType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterviewQuestionGenerationServiceTest {
    private final InterviewQuestionGenerationService service =
            new InterviewQuestionGenerationService(new ObjectMapper(), null);

    @Test
    void mixedPlanContainsTechnicalAndBehavioralQuestions() {
        MockInterviewSession session = session(UUID.randomUUID(), MockInterviewType.MIXED, 10);

        List<MockInterviewQuestion> questions = service.createQuestionPlan(session);

        assertTrue(questions.stream().anyMatch(question -> question.getQuestionType().equals("TECHNICAL")));
        assertTrue(questions.stream().anyMatch(question -> question.getQuestionType().equals("BEHAVIORAL")));
    }

    @Test
    void differentSessionsProduceMoreThanOneQuestionPlan() {
        Set<String> plans = new HashSet<>();
        for (int index = 0; index < 12; index++) {
            MockInterviewSession session = session(UUID.randomUUID(), MockInterviewType.MIXED, 10);
            plans.add(service.createQuestionPlan(session).stream()
                    .map(MockInterviewQuestion::getQuestionText)
                    .reduce("", (left, right) -> left + "|" + right));
        }

        assertTrue(plans.size() > 1);
    }

    @Test
    void fallbackPlanMatchesConfiguredDuration() {
        assertEquals(3, service.createQuestionPlan(
                session(UUID.randomUUID(), MockInterviewType.MIXED, 5)).size());
        assertEquals(5, service.createQuestionPlan(
                session(UUID.randomUUID(), MockInterviewType.MIXED, 10)).size());
        assertEquals(7, service.createQuestionPlan(
                session(UUID.randomUUID(), MockInterviewType.MIXED, 15)).size());
    }

    @Test
    void followUpUsesAnswerContextInsteadOfAlwaysRequestingNumbers() {
        MockInterviewQuestion technical = MockInterviewQuestion.builder()
                .id(UUID.randomUUID())
                .competency("TECHNICAL_DECISION")
                .build();
        MockInterviewQuestion teamwork = MockInterviewQuestion.builder()
                .id(UUID.randomUUID())
                .competency("COLLABORATION")
                .build();

        String technicalFollowUp = service.createFollowUp(
                technical,
                "Tôi chọn Redis để cache dữ liệu vì truy vấn cơ sở dữ liệu đang chậm và cần giảm tải.");
        String teamworkFollowUp = service.createFollowUp(
                teamwork,
                "Nhóm chúng tôi cùng xây dựng tính năng và hoàn thành đúng kế hoạch của sprint.");

        assertFalse(technicalFollowUp.startsWith("Kết quả của việc đó"));
        String normalizedTeamworkFollowUp = teamworkFollowUp.toLowerCase();
        assertTrue(
                normalizedTeamworkFollowUp.contains("bạn")
                        || normalizedTeamworkFollowUp.contains("đóng góp"),
                teamworkFollowUp
        );
    }

    private MockInterviewSession session(UUID id, MockInterviewType type, int duration) {
        return MockInterviewSession.builder()
                .id(id)
                .job(Job.builder().title("Backend Developer").build())
                .interviewType(type)
                .plannedDurationMinutes(duration)
                .build();
    }
}
