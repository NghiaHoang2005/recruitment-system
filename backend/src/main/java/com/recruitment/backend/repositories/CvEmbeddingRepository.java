package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.CvEmbedding;
import com.recruitment.backend.domain.entities.Cv.EmbeddingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CvEmbeddingRepository extends JpaRepository<CvEmbedding, UUID> {
    List<CvEmbedding> findByCvId(UUID cvId);

    List<CvEmbedding> findByType(EmbeddingType type);

    @Query(value = """
        SELECT DISTINCT e.cv_id 
        FROM cv_embeddings e 
        WHERE e.type = :type 
        ORDER BY e.vector <=> cast(:queryVector as vector) 
        LIMIT :topK
        """, nativeQuery = true)
    List<UUID> findTopMatchingCvIds(
            @Param("queryVector") String queryVector,
            @Param("type") String type,
            @Param("topK") int topK
    );
}
