package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.MockInterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MockInterviewFeedbackRepository extends JpaRepository<MockInterviewFeedback, UUID> {
    Optional<MockInterviewFeedback> findBySession_Id(UUID sessionId);
}
