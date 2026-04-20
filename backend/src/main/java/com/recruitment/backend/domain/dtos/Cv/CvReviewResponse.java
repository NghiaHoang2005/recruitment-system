package com.recruitment.backend.domain.dtos.Cv;

import com.recruitment.backend.domain.entities.Cv.CvReviewStatus;
import com.recruitment.backend.domain.entities.Cv.CvReviewType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CvReviewResponse {
    private UUID reviewId;
    private UUID cvId;
    private UUID jobId;
    private CvReviewType reviewType;
    private CvReviewStatus status;
    private Integer fitScore;
    private String summary;
    private String strengths;
    private String weaknesses;
    private String improvements;
    private String matchedRequirements;
    private String missingRequirements;
    private String actionPlan;
    private String provider;
    private String modelName;
    private String promptVersion;
    private String errorMessage;
    private LocalDateTime createdAt;
}
