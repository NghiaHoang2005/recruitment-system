package com.recruitment.backend.services.mockinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.domain.dtos.mockinterview.MockInterviewDtos.*;
import com.recruitment.backend.domain.entities.*;
import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.enums.MockInterviewStatus;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.*;
import com.recruitment.backend.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockInterviewService {
    private static final String DISCLAIMER =
            "Điểm số chỉ là ước tính cho mục đích luyện tập, không phải kết quả tuyển dụng.";

    private final MockInterviewSessionRepository sessionRepository;
    private final MockInterviewQuestionRepository questionRepository;
    private final MockInterviewTurnRepository turnRepository;
    private final MockInterviewFeedbackRepository feedbackRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final SecurityUtil securityUtil;
    private final InterviewQuestionGenerationService questionGenerationService;
    private final InterviewFeedbackService feedbackService;
    private final ObjectMapper objectMapper;

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionResponse create(CreateRequest request) {
        validateDuration(request.plannedDurationMinutes());
        UUID candidateId = securityUtil.getCurrentUser().getId();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        int softLimit = request.plannedDurationMinutes() * 60;
        int hardLimit = softLimit + (request.plannedDurationMinutes() == 15 ? 180 : 120);
        MockInterviewSession session = MockInterviewSession.builder()
                .candidate(candidate)
                .job(job)
                .interviewType(request.interviewType())
                .language(request.language() == null ? "vi" : request.language())
                .plannedDurationMinutes(request.plannedDurationMinutes())
                .softLimitSeconds(softLimit)
                .hardLimitSeconds(hardLimit)
                .status(MockInterviewStatus.CREATED)
                .promptVersion("mock_interview_v1")
                .build();
        sessionRepository.save(session);

        List<MockInterviewQuestion> questions = questionGenerationService.createQuestionPlan(session);
        questionRepository.saveAll(questions);
        session.getQuestions().addAll(questions);
        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<SessionResponse> list() {
        UUID candidateId = securityUtil.getCurrentUser().getId();
        return sessionRepository.findByCandidate_UserIdOrderByCreatedAtDesc(candidateId).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionResponse get(UUID sessionId) {
        return toSessionResponse(requireOwnedSession(sessionId));
    }

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public SessionResponse start(UUID sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (session.getStatus() == MockInterviewStatus.CREATED) {
            session.setStatus(MockInterviewStatus.IN_PROGRESS);
            session.setStartedAt(LocalDateTime.now());
        } else if (session.getStatus() != MockInterviewStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }
        return toSessionResponse(session);
    }

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<TurnResponse> appendTurns(UUID sessionId, AppendTurnsRequest request) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (session.getStatus() != MockInterviewStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        int nextSequence = turnRepository.findMaxSequenceNumber(sessionId) + 1;
        for (TurnRequest item : request.turns()) {
            if (turnRepository.findBySession_IdAndClientEventId(sessionId, item.clientEventId()).isPresent()) {
                continue;
            }
            MockInterviewQuestion question = null;
            if (item.questionId() != null) {
                question = questionRepository.findById(item.questionId())
                        .filter(value -> value.getSession().getId().equals(sessionId))
                        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
            }
            turnRepository.save(MockInterviewTurn.builder()
                    .session(session)
                    .question(question)
                    .clientEventId(item.clientEventId())
                    .sequenceNumber(nextSequence++)
                    .speaker(item.speaker())
                    .content(item.content().trim())
                    .startedOffsetMs(item.startedOffsetMs())
                    .endedOffsetMs(item.endedOffsetMs())
                    .finalTurn(true)
                    .build());
        }
        return transcript(sessionId);
    }

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public QuestionResponse createFollowUp(UUID sessionId, FollowUpRequest request) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (session.getStatus() != MockInterviewStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }
        if (pastSoftLimit(session)) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        MockInterviewQuestion parent = questionRepository.findById(request.parentQuestionId())
                .filter(question -> question.getSession().getId().equals(sessionId))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        if (questionRepository.countByParentQuestion_Id(parent.getId()) >= 2) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        int sequence = questionRepository.findBySession_IdOrderBySequenceNumber(sessionId).stream()
                .mapToInt(MockInterviewQuestion::getSequenceNumber)
                .max()
                .orElse(0) + 1;
        MockInterviewQuestion followUp = questionRepository.save(MockInterviewQuestion.builder()
                .session(session)
                .sequenceNumber(sequence)
                .questionType("FOLLOW_UP")
                .questionText(questionGenerationService.createFollowUp(parent, request.answer()))
                .competency(parent.getCompetency())
                .expectedTopics(parent.getExpectedTopics())
                .rubric(parent.getRubric())
                .followUp(true)
                .parentQuestion(parent)
                .build());
        return toQuestionResponse(followUp);
    }

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResultResponse complete(UUID sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (session.getStatus() == MockInterviewStatus.COMPLETED) {
            return result(session);
        }
        if (session.getStatus() != MockInterviewStatus.IN_PROGRESS
                && session.getStatus() != MockInterviewStatus.CREATED) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }

        session.setStatus(MockInterviewStatus.PROCESSING_FEEDBACK);
        List<MockInterviewTurn> turns =
                turnRepository.findBySession_IdAndFinalTurnTrueOrderBySequenceNumber(sessionId);
        MockInterviewFeedback feedback = feedbackService.generate(session, turns);
        feedbackRepository.save(feedback);
        session.setFeedback(feedback);
        session.setStatus(MockInterviewStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        if (session.getStartedAt() != null) {
            session.setActualDurationSeconds((int) Duration.between(
                    session.getStartedAt(), session.getEndedAt()).toSeconds());
        }
        return result(session);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResultResponse getResult(UUID sessionId) {
        MockInterviewSession session = requireOwnedSession(sessionId);
        if (session.getStatus() != MockInterviewStatus.COMPLETED) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }
        return result(session);
    }

    @Transactional
    @PreAuthorize("hasRole('CANDIDATE')")
    public void delete(UUID sessionId) {
        sessionRepository.delete(requireOwnedSession(sessionId));
    }

    private ResultResponse result(MockInterviewSession session) {
        MockInterviewFeedback feedback = feedbackRepository.findBySession_Id(session.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        return new ResultResponse(
                toSessionResponse(session),
                feedback.getOverallScore(),
                feedback.getScoreLabel(),
                feedback.getConfidence(),
                feedback.getOverallSummary(),
                readJson(feedback.getCriteriaScores()),
                readJson(feedback.getStrengths()),
                readJson(feedback.getImprovements()),
                readJson(feedback.getNextSteps()),
                readJson(feedback.getQuestionFeedback()),
                transcript(session.getId()),
                DISCLAIMER
        );
    }

    private List<TurnResponse> transcript(UUID sessionId) {
        return turnRepository.findBySession_IdAndFinalTurnTrueOrderBySequenceNumber(sessionId).stream()
                .map(turn -> new TurnResponse(
                        turn.getId(),
                        turn.getQuestion() == null ? null : turn.getQuestion().getId(),
                        turn.getSequenceNumber(),
                        turn.getSpeaker(),
                        turn.getContent(),
                        turn.getStartedOffsetMs(),
                        turn.getEndedOffsetMs()))
                .toList();
    }

    private MockInterviewSession requireOwnedSession(UUID sessionId) {
        UUID candidateId = securityUtil.getCurrentUser().getId();
        return sessionRepository.findByIdAndCandidate_UserId(sessionId, candidateId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    private SessionResponse toSessionResponse(MockInterviewSession session) {
        List<QuestionResponse> questions = questionRepository
                .findBySession_IdOrderBySequenceNumber(session.getId()).stream()
                .map(this::toQuestionResponse)
                .toList();
        return new SessionResponse(
                session.getId(),
                session.getJob().getId(),
                session.getJob().getTitle(),
                session.getJob().getCompany() == null ? null : session.getJob().getCompany().getName(),
                session.getInterviewType(),
                session.getLanguage(),
                session.getPlannedDurationMinutes(),
                session.getSoftLimitSeconds(),
                session.getHardLimitSeconds(),
                session.getActualDurationSeconds(),
                session.getStatus(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getCreatedAt(),
                questions);
    }

    private QuestionResponse toQuestionResponse(MockInterviewQuestion question) {
        return new QuestionResponse(
                question.getId(),
                question.getSequenceNumber(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getCompetency(),
                question.isFollowUp(),
                question.getParentQuestion() == null ? null : question.getParentQuestion().getId());
    }

    private boolean pastSoftLimit(MockInterviewSession session) {
        return session.getStartedAt() != null
                && Duration.between(session.getStartedAt(), LocalDateTime.now()).toSeconds()
                >= session.getSoftLimitSeconds();
    }

    private void validateDuration(Integer duration) {
        if (duration == null || (duration != 5 && duration != 10 && duration != 15)) {
            throw new AppException(ErrorCode.APPLICATION_INVALID_STATUS);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createArrayNode();
        }
    }
}
