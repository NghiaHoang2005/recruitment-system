package com.recruitment.backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mock_interview_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private MockInterviewSession session;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "score_label", nullable = false, length = 30)
    private String scoreLabel;

    @Column(nullable = false, length = 20)
    private String confidence;

    @Column(name = "overall_summary", nullable = false, columnDefinition = "TEXT")
    private String overallSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_scores", nullable = false, columnDefinition = "jsonb")
    private String criteriaScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String improvements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_steps", nullable = false, columnDefinition = "jsonb")
    private String nextSteps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_feedback", nullable = false, columnDefinition = "jsonb")
    private String questionFeedback;

    @Column(name = "schema_version", nullable = false, length = 30)
    private String schemaVersion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
