package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JobCategoryRepository extends JpaRepository<JobCategory, UUID> {
    List<JobCategory> findAllByOrderByDisplayOrderAsc();
    List<JobCategory> findByCodeIn(Collection<String> codes);
}
