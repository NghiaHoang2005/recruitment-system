package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobRequirementSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRequirementSectionRepository extends JpaRepository<JobRequirementSection, UUID> {
}
