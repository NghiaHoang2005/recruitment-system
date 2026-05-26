package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobSkill;
import com.recruitment.backend.domain.enums.RequirementSectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, UUID> {
    List<JobSkill> findByJob_Id(UUID jobId);

    List<JobSkill> findByJob_IdAndRequirementType(UUID jobId, RequirementSectionType requirementType);

    void deleteByJob_Id(UUID jobId);
}
