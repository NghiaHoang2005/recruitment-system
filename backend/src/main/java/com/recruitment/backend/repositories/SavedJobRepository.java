package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.SavedJob;
import com.recruitment.backend.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {
    List<SavedJob> findByCandidate_UserIdOrderBySavedAtDesc(UUID candidateId);

    Optional<SavedJob> findByCandidate_UserIdAndJob_Id(UUID candidateId, UUID jobId);

    boolean existsByCandidate_UserIdAndJob_Id(UUID candidateId, UUID jobId);

    long countByCandidate_UserIdAndJob_Status(UUID candidateId, JobStatus status);

    void deleteByCandidate_UserIdAndJob_Id(UUID candidateId, UUID jobId);
}
