package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Matching.MatchingRelevancePair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingRelevancePairRepository extends JpaRepository<MatchingRelevancePair, UUID> {
    List<MatchingRelevancePair> findByDataset_Id(UUID datasetId);
    void deleteByDataset_Id(UUID datasetId);
}
