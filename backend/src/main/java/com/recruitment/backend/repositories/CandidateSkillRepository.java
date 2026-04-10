package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Candidate.CandidateSkill;
import com.recruitment.backend.domain.entities.Candidate.CandidateSkillId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, CandidateSkillId> {

    @Modifying
    @Query("DELETE FROM CandidateSkill cs WHERE cs.candidate.userId = :candidateId")
    void deleteAllByCandidateId(UUID candidateId);
}
