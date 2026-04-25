package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvRepository extends JpaRepository<Cv, UUID> {
    List<Cv> findByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(UUID candidateId);

    Optional<Cv> findByIdAndCandidateUserId(UUID cvId, UUID candidateId);

    Optional<Cv> findTopByCandidateUserIdOrderByUploadedAtDesc(UUID candidateId);

    @Modifying
    @Query("""
            update Cv c
            set c.isDefault = false
            where c.candidate.userId = :candidateId and c.isDefault = true
            """)
    void clearDefaultByCandidateId(@Param("candidateId") UUID candidateId);
}
