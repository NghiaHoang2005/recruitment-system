package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.MockInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MockInterviewQuestionRepository extends JpaRepository<MockInterviewQuestion, UUID> {
    List<MockInterviewQuestion> findBySession_IdOrderBySequenceNumber(UUID sessionId);
    long countByParentQuestion_Id(UUID parentQuestionId);
}
