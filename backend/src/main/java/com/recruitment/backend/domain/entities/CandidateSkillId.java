package com.recruitment.backend.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkillId implements Serializable {

    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(name = "skill_id")
    private Long skillId;
}
