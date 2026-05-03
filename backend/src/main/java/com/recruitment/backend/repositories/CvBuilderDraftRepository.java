package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvBuilderDraftRepository extends JpaRepository<CvBuilderDraft, UUID> {
    List<CvBuilderDraft> findByCandidateUserIdOrderByUpdatedAtDesc(UUID candidateId);

    Optional<CvBuilderDraft> findByIdAndCandidateUserId(UUID draftId, UUID candidateId);
}
