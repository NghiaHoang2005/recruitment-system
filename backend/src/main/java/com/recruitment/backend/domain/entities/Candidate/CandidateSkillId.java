package com.recruitment.backend.domain.entities.Candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkillId implements Serializable {

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "skill_id")
    private UUID skillId;
}
