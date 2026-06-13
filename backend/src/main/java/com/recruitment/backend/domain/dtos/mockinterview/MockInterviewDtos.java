package com.recruitment.backend.domain.dtos.mockinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.recruitment.backend.domain.enums.InterviewSpeaker;
import com.recruitment.backend.domain.enums.MockInterviewStatus;
import com.recruitment.backend.domain.enums.MockInterviewType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class MockInterviewDtos {
    private MockInterviewDtos() {
    }

    public record CreateRequest(
            @NotNull UUID jobId,
            @NotNull MockInterviewType interviewType,
            @Pattern(regexp = "vi|en") String language,
            @NotNull @Min(5) @Max(15) Integer plannedDurationMinutes
    ) {
    }

    public record QuestionResponse(
            UUID id,
            int sequenceNumber,
            String questionType,
            String questionText,
            String competency,
            boolean followUp,
            UUID parentQuestionId
    ) {
    }

    public record TurnRequest(
            @NotNull UUID clientEventId,
            @NotNull @Min(1) Integer sequenceNumber,
            UUID questionId,
            @NotNull InterviewSpeaker speaker,
            @NotBlank @Size(max = 10000) String content,
            Integer startedOffsetMs,
            Integer endedOffsetMs
    ) {
    }

    public record AppendTurnsRequest(@NotEmpty List<@Valid TurnRequest> turns) {
    }

    public record FollowUpRequest(
            @NotNull UUID parentQuestionId,
            @NotBlank @Size(max = 10000) String answer
    ) {
    }

    public record SessionResponse(
            UUID id,
            UUID jobId,
            String jobTitle,
            String companyName,
            MockInterviewType interviewType,
            String language,
            int plannedDurationMinutes,
            int softLimitSeconds,
            int hardLimitSeconds,
            Integer actualDurationSeconds,
            MockInterviewStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            LocalDateTime createdAt,
            List<QuestionResponse> questions
    ) {
    }

    public record TurnResponse(
            UUID id,
            UUID questionId,
            int sequenceNumber,
            InterviewSpeaker speaker,
            String content,
            Integer startedOffsetMs,
            Integer endedOffsetMs
    ) {
    }

    public record ResultResponse(
            SessionResponse session,
            int overallScore,
            String scoreLabel,
            String confidence,
            String overallSummary,
            JsonNode criteriaScores,
            JsonNode strengths,
            JsonNode improvements,
            JsonNode nextSteps,
            JsonNode questionFeedback,
            List<TurnResponse> transcript,
            String disclaimer
    ) {
    }
}
