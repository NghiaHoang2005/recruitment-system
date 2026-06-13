package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.MockInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockInterviewSessionRepository extends JpaRepository<MockInterviewSession, UUID> {
    Optional<MockInterviewSession> findByIdAndCandidate_UserId(UUID id, UUID candidateId);
    List<MockInterviewSession> findByCandidate_UserIdOrderByCreatedAtDesc(UUID candidateId);
}
