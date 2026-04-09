package com.recruitment.backend.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "candidate_skills")
public class CandidateSkill {

    @EmbeddedId
    private CandidateSkillId id = new CandidateSkillId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("candidateId")
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    public CandidateSkill(Candidate candidate, Skill skill) {
        this.candidate = candidate;
        this.skill = skill;
        this.id.setCandidateId(candidate.getUserId());
        this.id.setSkillId(skill.getId());
    }
}
