package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByRecruiterId(UUID recruiterId);
    long countByCompany_IdAndStatusIn(UUID companyId, Collection<JobStatus> statuses);

    @Query(value = """
        SELECT cast(j.id as varchar) AS jobId,
               ts_rank(j.search_tsv, plainto_tsquery('simple', :query)) AS rank
        FROM jobs j
        WHERE j.search_tsv @@ plainto_tsquery('simple', :query)
          AND (:status IS NULL OR j.status = :status)
        ORDER BY rank DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<JobFtsView> searchJobsByFts(
            @Param("query") String query,
            @Param("status") String status,
            @Param("limit") int limit
    );

    interface JobFtsView {
        String getJobId();
        Double getRank();
    }

    long countByStatus(JobStatus status);

    List<Job> findTop5ByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findTop5ByOrderByCreatedAtDesc();
}
