package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Matching.MatchingRelevanceDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingRelevanceDatasetRepository extends JpaRepository<MatchingRelevanceDataset, UUID> {
    List<MatchingRelevanceDataset> findByCompanyId(UUID companyId);
}
