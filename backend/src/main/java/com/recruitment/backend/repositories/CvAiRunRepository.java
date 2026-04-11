package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.CvAiRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CvAiRunRepository extends JpaRepository<CvAiRun, UUID> {
}
