package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {
    void deleteByJob_Id(UUID jobId);

    List<JobEmbedding> findByJob_Id(UUID jobId);

    @Query(value = """
        SELECT e.job_id AS jobId,
               (e.vector <=> cast(:queryVector as vector)) AS distance
        FROM job_embeddings e
        JOIN jobs j ON j.id = e.job_id
        WHERE e.embedding_type = :type
          AND e.model = :model
          AND e.dimensions = :dimensions
          AND j.status = :status
        ORDER BY e.vector <=> cast(:queryVector as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<JobEmbeddingScoreView> findTopJobScoresByTypeModelDimensionsAndStatus(
            @Param("queryVector") String queryVector,
            @Param("type") String type,
            @Param("model") String model,
            @Param("dimensions") int dimensions,
            @Param("status") String status,
            @Param("topK") int topK
    );

    interface JobEmbeddingScoreView {
        UUID getJobId();
        Double getDistance();
    }
}
