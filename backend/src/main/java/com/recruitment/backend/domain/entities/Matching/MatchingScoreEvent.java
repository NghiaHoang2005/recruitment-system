package com.recruitment.backend.domain.entities.Matching;

import com.recruitment.backend.domain.enums.MatchingRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matching_score_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingScoreEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private MatchingRequestType requestType;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "cv_id")
    private UUID cvId;

    @Column(name = "fit_score")
    private Double fitScore;

    @Column(name = "semantic_score")
    private Double semanticScore;

    @Column(name = "fts_score")
    private Double ftsScore;

    @Column(name = "skill_score")
    private Double skillScore;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
