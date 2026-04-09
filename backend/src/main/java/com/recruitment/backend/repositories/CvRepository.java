package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {
        Cv findByCandidateId(Long candidateId);
}
