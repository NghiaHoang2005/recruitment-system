package com.recruitment.backend.domain.entities.Matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "matching_evaluation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingEvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "weight_profile_id")
    private UUID weightProfileId;

    @Column(name = "weight_version")
    private String weightVersion;

    @Column(name = "top_k", nullable = false)
    private Integer topK;

    @Column(name = "precision_at_k")
    private Double precisionAtK;

    @Column(name = "recall_at_k")
    private Double recallAtK;

    @Column(name = "f1_at_k")
    private Double f1AtK;

    @Column(name = "total_queries", nullable = false)
    private Integer totalQueries;

    @Column(name = "total_relevant", nullable = false)
    private Integer totalRelevant;

    @Column(name = "evaluated_pairs")
    private Integer evaluatedPairs;

    @Column(name = "successful_pairs")
    private Integer successfulPairs;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private String version;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (evaluatedAt == null) {
            evaluatedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }
}
