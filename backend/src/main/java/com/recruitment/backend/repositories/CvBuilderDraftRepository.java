package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvBuilderDraftRepository extends JpaRepository<CvBuilderDraft, UUID> {
    List<CvBuilderDraft> findByCandidateUserIdOrderByUpdatedAtDesc(UUID candidateId);

    Optional<CvBuilderDraft> findByIdAndCandidateUserId(UUID draftId, UUID candidateId);

    @Query("""
        SELECT d FROM CvBuilderDraft d
        WHERE d.candidate.userId = :candidateId
          AND (d.updatedAt < :updatedAt OR (d.updatedAt = :updatedAt AND d.id < :draftId))
        ORDER BY d.updatedAt DESC, d.id DESC
        """)
    List<CvBuilderDraft> findNextDrafts(
            @Param("candidateId") UUID candidateId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt,
            @Param("draftId") UUID draftId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        SELECT d FROM CvBuilderDraft d
        WHERE d.candidate.userId = :candidateId
        ORDER BY d.updatedAt DESC, d.id DESC
        """)
    List<CvBuilderDraft> findFirstDrafts(
            @Param("candidateId") UUID candidateId,
            org.springframework.data.domain.Pageable pageable
    );
}
