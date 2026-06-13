package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.MockInterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockInterviewTurnRepository extends JpaRepository<MockInterviewTurn, UUID> {
    List<MockInterviewTurn> findBySession_IdAndFinalTurnTrueOrderBySequenceNumber(UUID sessionId);
    Optional<MockInterviewTurn> findBySession_IdAndClientEventId(UUID sessionId, UUID clientEventId);

    @Query("select coalesce(max(t.sequenceNumber), 0) from MockInterviewTurn t where t.session.id = :sessionId")
    int findMaxSequenceNumber(@Param("sessionId") UUID sessionId);
}
