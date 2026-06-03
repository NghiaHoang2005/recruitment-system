package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.domain.enums.PipelineJobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pipeline_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private PipelineJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineJobStatus status;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "total_items")
    private int totalItems;

    @Column(name = "processed_items")
    private int processedItems;

    @Column(name = "failed_items")
    private int failedItems;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
