package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Candidate;
import com.recruitment.backend.domain.entities.CandidateSkill;
import com.recruitment.backend.domain.entities.Skill;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import com.recruitment.backend.repositories.SkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateRepository candidateRepository;

    public List<Skill> processAndGetSkills(List<String> skillNames) {
        List<Skill> finalSkills = new ArrayList<>();

        for (String name : skillNames) {
            String normalizedName = name.trim().toLowerCase();

            Skill skill = skillRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(name.trim());
                        newSkill.setIsVerified(true);
                        return skillRepository.save(newSkill);
                    });
            finalSkills.add(skill);
        }
        return finalSkills;
    }

    @Transactional
    public void linkSkillsToCandidate(Long candidateId, List<String> rawSkillNames) {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate không tồn tại với ID: " + candidateId));

        List<Skill> validSkills = processAndGetSkills(rawSkillNames);

        candidateSkillRepository.deleteAllByCandidateId(candidateId);

        List<CandidateSkill> newMappings = validSkills.stream()
                .map(skill -> new CandidateSkill(candidate, skill))
                .collect(Collectors.toList());

        candidateSkillRepository.saveAll(newMappings);
    }
}