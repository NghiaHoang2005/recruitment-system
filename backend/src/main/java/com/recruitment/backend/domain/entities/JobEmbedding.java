package com.recruitment.backend.domain.entities;

import com.recruitment.backend.domain.enums.JobEmbeddingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobEmbeddingType embeddingType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    private Integer dimensions;

    private Integer tokenCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector")
    private float[] vector;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
