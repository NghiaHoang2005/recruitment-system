package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.entities.MockInterviewSession;
import com.recruitment.backend.domain.entities.MockInterviewTurn;
import com.recruitment.backend.domain.enums.InterviewSpeaker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterviewFeedbackServiceTest {
    private final InterviewFeedbackService service = new InterviewFeedbackService(new ObjectMapper(), null);

    @Test
    void generatesBoundedScoreAndStructuredCriteria() {
        MockInterviewSession session = MockInterviewSession.builder().id(UUID.randomUUID()).build();
        List<MockInterviewTurn> turns = List.of(
                MockInterviewTurn.builder()
                        .speaker(InterviewSpeaker.CANDIDATE)
                        .finalTurn(true)
                        .content("Toi phan tich log, toi uu truy van va giam thoi gian phan hoi 40 phan tram.")
                        .build(),
                MockInterviewTurn.builder()
                        .speaker(InterviewSpeaker.CANDIDATE)
                        .finalTurn(true)
                        .content("Toi trao doi voi nhom, chia nho cong viec va hoan thanh dung han.")
                        .build()
        );

        var feedback = service.generate(session, turns);

        assertTrue(feedback.getOverallScore() >= 0 && feedback.getOverallScore() <= 100);
        assertNotNull(feedback.getScoreLabel());
        assertTrue(feedback.getCriteriaScores().contains("CONTENT_KNOWLEDGE"));
        assertEquals("mock_interview_feedback_v1", feedback.getSchemaVersion());
    }

    @Test
    void returnsLowConfidenceWhenTranscriptIsEmpty() {
        MockInterviewSession session = MockInterviewSession.builder().id(UUID.randomUUID()).build();

        var feedback = service.generate(session, List.of());

        assertEquals("LOW", feedback.getConfidence());
        assertFalse(feedback.getImprovements().isBlank());
    }
}
