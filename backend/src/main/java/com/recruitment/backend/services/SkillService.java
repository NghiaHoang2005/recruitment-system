package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Candidate.Candidate;
import com.recruitment.backend.domain.entities.Candidate.CandidateSkill;
import com.recruitment.backend.domain.entities.Skill;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.CandidateRepository;
import com.recruitment.backend.repositories.CandidateSkillRepository;
import com.recruitment.backend.repositories.SkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {
    private final SkillRepository skillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final CandidateRepository candidateRepository;

    public List<Skill> processAndGetSkills(List<String> skillNames) {
        List<String> normalizedNames = skillNames.stream()
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
        List<Skill> existingSkills =
                skillRepository.findAllByNameInIgnoreCase(normalizedNames);
        Map<String, Skill> existingMap = existingSkills.stream()
                .collect(Collectors.toMap(
                        s -> s.getName().toLowerCase(),
                        Function.identity()
                ));
        List<Skill> newSkills = normalizedNames.stream()
                .filter(name -> !existingMap.containsKey(name))
                .map(name -> {
                    Skill skill = new Skill();
                    skill.setName(name);
                    skill.setIsVerified(true);
                    return skill;
                })
                .toList();
        if (!newSkills.isEmpty()) {
            skillRepository.saveAll(newSkills);
        }

        List<Skill> result = new ArrayList<>(existingSkills);
        result.addAll(newSkills);

        return result;
    }

    @Transactional
    public void linkSkillsToCandidate(UUID candidateId, List<String> rawSkillNames) {

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new AppException(ErrorCode.CANDIDATE_NOT_FOUND));

        List<Skill> newSkills = processAndGetSkills(rawSkillNames);

        List<CandidateSkill> currentMappings =
                candidateSkillRepository.findByCandidateUserId(candidateId);

        Set<UUID> currentSkillIds = currentMappings.stream()
                .map(cs -> cs.getSkill().getId())
                .collect(Collectors.toSet());

        Set<UUID> newSkillIds = newSkills.stream()
                .map(Skill::getId)
                .collect(Collectors.toSet());
        List<CandidateSkill> toDelete = currentMappings.stream()
                .filter(cs -> !newSkillIds.contains(cs.getSkill().getId()))
                .toList();

        candidateSkillRepository.deleteAll(toDelete);

        List<CandidateSkill> toInsert = newSkills.stream()
                .filter(skill -> !currentSkillIds.contains(skill.getId()))
                .map(skill -> new CandidateSkill(candidate, skill))
                .toList();

        candidateSkillRepository.saveAll(toInsert);
    }
}