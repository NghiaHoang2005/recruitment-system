package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByRecruiterId(UUID recruiterId);
    long countByCompany_IdAndStatusIn(UUID companyId, Collection<JobStatus> statuses);
}
