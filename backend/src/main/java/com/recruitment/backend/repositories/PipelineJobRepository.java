package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.PipelineJob;
import com.recruitment.backend.domain.enums.PipelineJobStatus;
import com.recruitment.backend.domain.enums.PipelineJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PipelineJobRepository extends JpaRepository<PipelineJob, UUID> {

    long countByStatus(PipelineJobStatus status);

    List<PipelineJob> findByStatusIn(List<PipelineJobStatus> statuses);

    List<PipelineJob> findByStatusOrderByCreatedAtDesc(PipelineJobStatus status);

    List<PipelineJob> findTop20ByOrderByCreatedAtDesc();

    boolean existsByJobTypeAndStatusIn(PipelineJobType jobType, List<PipelineJobStatus> statuses);

    @Query("SELECT COUNT(p) FROM PipelineJob p WHERE p.status = 'RUNNING' AND p.startedAt < :threshold")
    long countStalledJobs(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT p FROM PipelineJob p WHERE p.status = 'RUNNING' AND p.startedAt < :threshold")
    List<PipelineJob> findStalledJobs(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT MAX(p.completedAt) FROM PipelineJob p WHERE p.status = 'COMPLETED'")
    LocalDateTime findLatestCompletionTime();
}
