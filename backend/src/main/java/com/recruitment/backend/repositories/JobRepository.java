package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.CompanyStatus;
import com.recruitment.backend.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    List<Job> findByRecruiterId(UUID recruiterId);

    @EntityGraph(attributePaths = {"company", "categories"})
    Page<Job> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company", "categories"})
    Page<Job> findByRecruiterId(UUID recruiterId, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "categories"})
    Page<Job> findByCompany_Id(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "categories"})
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    List<Job> findByRecruiterIdOrderByCreatedAtDesc(UUID recruiterId);
    List<Job> findByCompany_IdOrderByCreatedAtDesc(UUID companyId);
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);
    long countByCompany_IdAndStatusIn(UUID companyId, Collection<JobStatus> statuses);

    @Query(value = """
        SELECT cast(j.id as varchar) AS jobId,
               CASE
                   WHEN :hasQuery THEN ts_rank(j.search_tsv, websearch_to_tsquery('simple', :query))
                   ELSE 0
               END AS rank
        FROM jobs j
        WHERE (
            :hasQuery = false
            OR (
                j.search_tsv @@ websearch_to_tsquery('simple', :query)
                OR j.title ILIKE concat('%', :query, '%')
                OR EXISTS (
                    SELECT 1 FROM job_skills js
                    JOIN skills s ON s.id = js.skill_id
                    WHERE js.job_id = j.id AND s.name ILIKE concat('%', :query, '%')
                )
            )
        )
        AND (:status IS NULL OR j.status = :status)
        AND (
            :categoryCode IS NULL
            OR EXISTS (
                SELECT 1
                FROM job_category_mappings jcm
                JOIN job_categories jc ON jc.id = jcm.category_id
                WHERE jcm.job_id = j.id AND jc.code = :categoryCode
            )
        )
        AND (
            :locations IS NULL
            OR EXISTS (
                SELECT 1
                FROM locations l
                WHERE l.id = j.location_id
                  AND l.code = ANY(string_to_array(CAST(:locations AS text), '||'))
            )
        )
        AND (
            :employmentTypes IS NULL
            OR j.employment_type = ANY(string_to_array(CAST(:employmentTypes AS text), '||'))
        )
        AND (
            :workModes IS NULL
            OR j.work_mode = ANY(string_to_array(CAST(:workModes AS text), '||'))
        )
        AND (
            :levels IS NULL
            OR j.level = ANY(string_to_array(CAST(:levels AS text), '||'))
        )
        AND (
            :salaryNegotiable IS NULL
            OR j.salary_negotiable = :salaryNegotiable
        )
        AND (
            :salaryMin IS NULL
            OR COALESCE(j.max_salary, j.min_salary, 0) >= :salaryMin
        )
        AND (
            :salaryMax IS NULL
            OR COALESCE(j.min_salary, j.max_salary, 0) <= :salaryMax
        )
        ORDER BY
            CASE WHEN :oldest THEN j.created_at END ASC,
            CASE WHEN :salaryDesc THEN COALESCE(j.max_salary, j.min_salary, 0) END DESC,
            CASE
                WHEN :hasQuery AND NOT :oldest AND NOT :salaryDesc
                THEN ts_rank(j.search_tsv, websearch_to_tsquery('simple', :query))
            END DESC,
            j.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<JobFtsView> searchJobsByFts(
            @Param("query") String query,
            @Param("hasQuery") boolean hasQuery,
            @Param("status") String status,
            @Param("categoryCode") String categoryCode,
            @Param("locations") String locations,
            @Param("employmentTypes") String employmentTypes,
            @Param("workModes") String workModes,
            @Param("levels") String levels,
            @Param("salaryMin") Integer salaryMin,
            @Param("salaryMax") Integer salaryMax,
            @Param("salaryNegotiable") Boolean salaryNegotiable,
            @Param("oldest") boolean oldest,
            @Param("salaryDesc") boolean salaryDesc,
            @Param("limit") int limit
    );

    interface JobFtsView {
        String getJobId();
        Double getRank();
    }

    long countByStatus(JobStatus status);

    List<Job> findTop5ByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findTop5ByOrderByCreatedAtDesc();

    List<Job> findByCompany_IdAndStatus(UUID companyId, JobStatus status);

    long countByCreatedAtAfter(LocalDateTime createdAt);

    long countByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);

    List<Job> findByCreatedAtAfter(LocalDateTime createdAt);

    List<Job> findByCreatedAtBetween(LocalDateTime fromDate, LocalDateTime toDate);
}
