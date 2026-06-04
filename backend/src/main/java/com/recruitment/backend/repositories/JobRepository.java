package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByRecruiterId(UUID recruiterId);
    long countByCompany_IdAndStatusIn(UUID companyId, Collection<JobStatus> statuses);

    @Query(value = """
        SELECT j.id AS jobId,
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
        UUID getJobId();
        Double getRank();
    }

    long countByStatus(JobStatus status);

    List<Job> findTop5ByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findTop5ByOrderByCreatedAtDesc();

    List<Job> findByCompany_IdAndStatus(UUID companyId, JobStatus status);

    long countByCreatedAtAfter(LocalDateTime createdAt);

    List<Job> findByCreatedAtAfter(LocalDateTime createdAt);

    @Query("""
            select j from Job j
            left join j.company c
            where (:status is null or j.status = :status)
              and (:companyStatus is null or c.status = :companyStatus)
              and (:fromDate is null or j.createdAt >= :fromDate)
              and (:toDate is null or j.createdAt <= :toDate)
              and (
                :keyword is null
                or lower(j.title) like lower(concat('%', :keyword, '%'))
                or lower(c.name) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Job> searchAdminJobs(
            @Param("keyword") String keyword,
            @Param("status") JobStatus status,
            @Param("companyStatus") CompanyStatus companyStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
