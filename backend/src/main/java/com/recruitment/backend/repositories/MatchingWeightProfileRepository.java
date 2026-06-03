package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Matching.MatchingWeightProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingWeightProfileRepository extends JpaRepository<MatchingWeightProfile, UUID> {

    Optional<MatchingWeightProfile> findFirstByCompanyIdAndActiveTrueOrderByUpdatedAtDesc(UUID companyId);

    Optional<MatchingWeightProfile> findFirstByCompanyIdIsNullAndActiveTrueOrderByUpdatedAtDesc();

    List<MatchingWeightProfile> findByCompanyIdOrderByUpdatedAtDesc(UUID companyId);

    @Modifying
    @Query("update MatchingWeightProfile p set p.active = false, p.updatedAt = CURRENT_TIMESTAMP " +
            "where p.companyId = :companyId and p.active = true")
    int deactivateActiveByCompany(@Param("companyId") UUID companyId);

    @Modifying
    @Query("update MatchingWeightProfile p set p.active = false, p.updatedAt = CURRENT_TIMESTAMP " +
            "where p.companyId is null and p.active = true")
    int deactivateActiveGlobal();
}
