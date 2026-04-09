package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.CandidateSkill;
import com.recruitment.backend.domain.entities.CandidateSkillId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, CandidateSkillId> {

    @Modifying
    @Query("DELETE FROM CandidateSkill cs WHERE cs.candidate.userId = :candidateId")
    void deleteAllByCandidateId(Long candidateId);
}
