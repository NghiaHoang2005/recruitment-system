package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Matching.MatchingScoreEvent;
import com.recruitment.backend.domain.enums.MatchingRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchingScoreEventRepository extends JpaRepository<MatchingScoreEvent, UUID> {

    Page<MatchingScoreEvent> findByRequestTypeOrderByCreatedAtDesc(MatchingRequestType requestType, Pageable pageable);

    long countByRequestType(MatchingRequestType requestType);

    @Query("SELECT AVG(m.latencyMs) FROM MatchingScoreEvent m WHERE m.requestType = :type")
    Double findAvgLatencyByType(@Param("type") MatchingRequestType type);

    @Query("SELECT AVG(m.latencyMs) FROM MatchingScoreEvent m")
    Double findOverallAvgLatency();

    @Query("SELECT m.latencyMs FROM MatchingScoreEvent m WHERE m.requestType = :type AND m.latencyMs IS NOT NULL ORDER BY m.latencyMs ASC")
    List<Long> findAllLatenciesByTypeOrderedAsc(@Param("type") MatchingRequestType type);

    @Query("SELECT m.latencyMs FROM MatchingScoreEvent m WHERE m.latencyMs IS NOT NULL ORDER BY m.latencyMs ASC")
    List<Long> findAllLatenciesOrderedAsc();

    long countByCreatedAtAfter(LocalDateTime after);
}
