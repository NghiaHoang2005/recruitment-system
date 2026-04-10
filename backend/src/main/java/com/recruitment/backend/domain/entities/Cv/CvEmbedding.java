package com.recruitment.backend.domain.entities.Cv;

import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "cv_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private Cv cv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmbeddingType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] vector;
}