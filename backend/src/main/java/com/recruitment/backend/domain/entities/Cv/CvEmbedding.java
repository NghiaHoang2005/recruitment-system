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

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "dimensions")
    private Integer dimensions;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "token_count")
    private Integer tokenCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector")
    private float[] vector;

    public String getProvider() {
        if (model == null) return "unknown";
        if (model.contains("gemini")) return "gemini";
        if (model.contains("embedding-3")) return "openai";
        if (model.contains("text-embedding")) return "openai";
        return "unknown";
    }

    public boolean isValidDimensions() {
        if (dimensions == null || vector == null) return false;
        return vector.length == dimensions && 
               (dimensions == 768 || dimensions == 1536 || dimensions == 3072);
    }
}

