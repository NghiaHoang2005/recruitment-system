package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {
    void deleteByJob_Id(UUID jobId);
}
