package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobReport;
import com.recruitment.backend.domain.enums.JobReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobReportRepository extends JpaRepository<JobReport, UUID> {
    boolean existsByReporter_IdAndJob_Id(UUID reporterId, UUID jobId);
    Page<JobReport> findByStatus(JobReportStatus status, Pageable pageable);
}
