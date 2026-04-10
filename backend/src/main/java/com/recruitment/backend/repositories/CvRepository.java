package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CvRepository extends JpaRepository<Cv, UUID> { }
