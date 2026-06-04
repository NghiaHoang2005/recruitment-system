package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CvEmbeddingRepository extends JpaRepository<CvEmbedding, UUID> {
    void deleteByCvId(UUID cvId);

    List<CvEmbedding> findByCvId(UUID cvId);

    List<CvEmbedding> findByCvIdIn(List<UUID> cvIds);

    List<CvEmbedding> findByType(EmbeddingType type);

    @Query(value = """
        SELECT cast(e.cv_id as varchar) 
        FROM cv_embeddings e 
        WHERE e.type = :type 
        GROUP BY e.cv_id
        ORDER BY MIN(e.vector <=> cast(:queryVector as vector)) 
        LIMIT :topK
        """, nativeQuery = true)
    List<String> findTopMatchingCvIds(
            @Param("queryVector") String queryVector,
            @Param("type") String type,
            @Param("topK") int topK
    );

    @Query(value = """
        SELECT cast(e.cv_id as varchar)
        FROM cv_embeddings e
        WHERE e.type = :type
          AND e.model = :model
          AND e.dimensions = :dimensions
        GROUP BY e.cv_id
        ORDER BY MIN(e.vector <=> cast(:queryVector as vector))
        LIMIT :topK
        """, nativeQuery = true)
    List<String> findTopMatchingCvIdsByTypeAndModelAndDimensions(
            @Param("queryVector") String queryVector,
            @Param("type") String type,
            @Param("model") String model,
            @Param("dimensions") int dimensions,
            @Param("topK") int topK
    );

    @Query(value = """
        SELECT cast(e.cv_id as varchar) AS cvId,
               MIN(e.vector <=> cast(:queryVector as vector)) AS distance
        FROM cv_embeddings e
        JOIN cvs c ON c.id = e.cv_id
        WHERE e.type = :type
          AND e.model = :model
          AND e.dimensions = :dimensions
          AND (:candidateId IS NULL OR c.candidate_id = :candidateId)
        GROUP BY e.cv_id
        ORDER BY distance
        LIMIT :topK
        """, nativeQuery = true)
    List<CvEmbeddingScoreView> findTopCvScoresByTypeModelDimensionsAndCandidate(
            @Param("queryVector") String queryVector,
            @Param("type") String type,
            @Param("model") String model,
            @Param("dimensions") int dimensions,
            @Param("candidateId") UUID candidateId,
            @Param("topK") int topK
    );

    interface CvEmbeddingScoreView {
        String getCvId();
        Double getDistance();
    }
}
