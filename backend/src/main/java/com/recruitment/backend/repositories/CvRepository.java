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

    Optional<Cv> findFirstByCandidateUserIdOrderByIsDefaultDescUploadedAtDesc(UUID candidateId);

    Optional<Cv> findTopByCandidateUserIdOrderByUploadedAtDesc(UUID candidateId);

    List<Cv> findByIdInAndCandidate_OpenToWorkTrue(List<UUID> cvIds);

    @Modifying
    @Query("""
            update Cv c
            set c.isDefault = false
            where c.candidate.userId = :candidateId and c.isDefault = true
            """)
    void clearDefaultByCandidateId(@Param("candidateId") UUID candidateId);

    @Query(value = """
        SELECT cast(c.id as varchar) AS cvId,
               ts_rank(c.search_tsv, plainto_tsquery('simple', :query)) AS rank
        FROM cvs c
        WHERE c.search_tsv @@ plainto_tsquery('simple', :query)
          AND (:candidateId IS NULL OR c.candidate_id = :candidateId)
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CvFtsView> searchCvsByFts(
            @Param("query") String query,
            @Param("candidateId") UUID candidateId,
            @Param("limit") int limit
    );

    interface CvFtsView {
        String getCvId();
        Double getRank();
    }
}
