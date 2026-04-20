package com.recruitment.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.recruitment.backend.domain.dtos.Cv.CvReviewResponse;
import com.recruitment.backend.domain.entities.Cv.Cv;
import com.recruitment.backend.domain.entities.Cv.CvReview;
import com.recruitment.backend.domain.entities.Cv.CvReviewStatus;
import com.recruitment.backend.domain.entities.Cv.CvReviewType;
import com.recruitment.backend.domain.entities.Cv.CvStatus;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CvRepository;
import com.recruitment.backend.repositories.CvReviewRepository;
import com.recruitment.backend.repositories.JobRepository;
import com.recruitment.backend.services.ai.pipeline.CvReviewAiService;
import com.recruitment.backend.services.ai.pipeline.TextNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvReviewService {

    private static final int DAILY_REVIEW_LIMIT = 3;

    private final CvRepository cvRepository;
    private final JobRepository jobRepository;
    private final CvReviewRepository cvReviewRepository;
    private final CvReviewAiService cvReviewAiService;
    private final TextNormalizationService textNormalizationService;

    @Transactional
    public CvReviewResponse createReview(UUID currentUserId, UUID cvId, UUID jobId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!CvStatus.COMPLETED.equals(cv.getAiStatus()) || cv.getParsedData() == null || cv.getParsedData().isBlank()) {
            throw new AppException(ErrorCode.CV_PROCESSING_FAILED);
        }

        enforceDailyRateLimit(currentUserId);

        Job job = null;
        CvReviewType type = CvReviewType.GENERAL;
        if (jobId != null) {
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
            type = CvReviewType.JOB_MATCH;
        }

        String language = textNormalizationService.detectLanguage(cv.getRawText() == null ? "" : cv.getRawText());

        CvReview review = CvReview.builder()
                .cv(cv)
                .candidate(cv.getCandidate())
                .job(job)
                .reviewType(type)
                .status(CvReviewStatus.FAILED)
                .build();

        try {
            CvReviewAiService.ReviewAiResult result = cvReviewAiService.review(cvId, cv, job, language);
            JsonNode node = result.getNode();

            review.setStatus(CvReviewStatus.COMPLETED);
            review.setSummary(asText(node, "summary"));
            review.setFitScore(asInt(node, "fit_score"));
            review.setStrengths(asJson(node, "strengths"));
            review.setWeaknesses(asJson(node, "weaknesses"));
            review.setImprovements(asJson(node, "improvements"));
            review.setMatchedRequirements(asJson(node, "matched_requirements"));
            review.setMissingRequirements(asJson(node, "missing_requirements"));
            review.setActionPlan(asJson(node, "action_plan"));
            review.setProvider(result.getProvider());
            review.setModelName(result.getModelName());
            review.setModelVersion(result.getModelVersion());
            review.setPromptVersion(result.getPromptVersion());
            review.setRawModelOutput(result.getRawJson());
            review.setErrorMessage(null);
        } catch (RuntimeException ex) {
            log.warn("CV review failed for cvId={}, userId={}: {}", cvId, currentUserId, ex.getMessage());
            review.setStatus(CvReviewStatus.FAILED);
            review.setErrorMessage(ex.getMessage());
        }

        CvReview saved = cvReviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CvReviewResponse getLatestReview(UUID currentUserId, UUID cvId, UUID jobId) {
        Cv cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_NOT_FOUND));

        if (!cv.getCandidate().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        CvReview review = cvReviewRepository.findLatestByCvIdAndJobId(cvId, jobId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_REVIEW_NOT_FOUND));

        return toResponse(review);
    }

    private void enforceDailyRateLimit(UUID currentUserId) {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        long count = cvReviewRepository.countByCandidateInDateRange(currentUserId, from);
        if (count >= DAILY_REVIEW_LIMIT) {
            throw new AppException(ErrorCode.CV_REVIEW_RATE_LIMIT_EXCEEDED);
        }
    }

    private String asText(JsonNode node, String field) {
        JsonNode f = node.path(field);
        return f.isMissingNode() || f.isNull() ? null : f.asText(null);
    }

    private Integer asInt(JsonNode node, String field) {
        JsonNode f = node.path(field);
        return f.isMissingNode() || f.isNull() ? null : f.asInt();
    }

    private String asJson(JsonNode node, String field) {
        JsonNode f = node.path(field);
        if (f.isMissingNode() || f.isNull()) {
            return null;
        }
        return f.toString();
    }

    private CvReviewResponse toResponse(CvReview review) {
        return CvReviewResponse.builder()
                .reviewId(review.getId())
                .cvId(review.getCv().getId())
                .jobId(review.getJob() != null ? review.getJob().getId() : null)
                .reviewType(review.getReviewType())
                .status(review.getStatus())
                .fitScore(review.getFitScore())
                .summary(review.getSummary())
                .strengths(review.getStrengths())
                .weaknesses(review.getWeaknesses())
                .improvements(review.getImprovements())
                .matchedRequirements(review.getMatchedRequirements())
                .missingRequirements(review.getMissingRequirements())
                .actionPlan(review.getActionPlan())
                .provider(review.getProvider())
                .modelName(review.getModelName())
                .promptVersion(review.getPromptVersion())
                .errorMessage(review.getErrorMessage())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
