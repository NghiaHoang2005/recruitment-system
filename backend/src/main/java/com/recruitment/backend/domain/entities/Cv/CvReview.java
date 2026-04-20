package com.recruitment.backend.domain.entities.Cv;

import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Job;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cv_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CvReviewType reviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CvReviewStatus status;

    @Column(name = "fit_score")
    private Integer fitScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private String strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "jsonb")
    private String weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvements", columnDefinition = "jsonb")
    private String improvements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_requirements", columnDefinition = "jsonb")
    private String missingRequirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_requirements", columnDefinition = "jsonb")
    private String matchedRequirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_plan", columnDefinition = "jsonb")
    private String actionPlan;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "raw_model_output", columnDefinition = "TEXT")
    private String rawModelOutput;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
