package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Matching.MatchingEvaluationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingEvaluationRunRepository extends JpaRepository<MatchingEvaluationRun, UUID> {
    List<MatchingEvaluationRun> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<MatchingEvaluationRun> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);
    List<MatchingEvaluationRun> findAllByOrderByCreatedAtDesc();
}
