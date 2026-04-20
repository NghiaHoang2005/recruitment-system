package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.CvReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvReviewRepository extends JpaRepository<CvReview, UUID> {

    @Query("""
            select r from CvReview r
            where r.cv.id = :cvId
              and ((:jobId is null and r.job is null) or (r.job.id = :jobId))
            order by r.createdAt desc
            """)
    Optional<CvReview> findLatestByCvIdAndJobId(@Param("cvId") UUID cvId, @Param("jobId") UUID jobId);

    @Query("""
            select count(r) from CvReview r
            where r.candidate.userId = :candidateId
              and r.createdAt >= :from
            """)
    long countByCandidateInDateRange(@Param("candidateId") UUID candidateId, @Param("from") LocalDateTime from);
}
