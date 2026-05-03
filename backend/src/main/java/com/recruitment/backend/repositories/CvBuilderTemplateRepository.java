package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.CvBuilder.CvBuilderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvBuilderTemplateRepository extends JpaRepository<CvBuilderTemplate, UUID> {
    List<CvBuilderTemplate> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<CvBuilderTemplate> findByIdAndIsActiveTrue(UUID id);
}
