package com.recruitment.backend.services;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobSkill;
import com.recruitment.backend.domain.entities.Skill;
import com.recruitment.backend.domain.enums.RequirementSectionType;
import com.recruitment.backend.repositories.JobSkillRepository;
import com.recruitment.backend.services.ai.model.JobExtractionResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobSkillExtractionService {

    private final JobSkillRepository jobSkillRepository;
    private final SkillService skillService;

    @Transactional
    public void replaceJobSkills(Job job, JobExtractionResult result) {
        if (job == null || job.getId() == null) {
            return;
        }

        Map<String, RequirementSectionType> skillLevels = new LinkedHashMap<>();
        addSkills(skillLevels, result.getRequiredSkills(), RequirementSectionType.REQUIRED);
        addSkills(skillLevels, result.getPreferredSkills(), RequirementSectionType.PREFERRED);
        addSkills(skillLevels, result.getOtherSkills(), RequirementSectionType.OTHER);

        if (skillLevels.isEmpty()) {
            jobSkillRepository.deleteByJob_Id(job.getId());
            return;
        }

        List<String> normalizedNames = new ArrayList<>(skillLevels.keySet());
        List<Skill> skills = skillService.processAndGetSkills(normalizedNames);
        Map<String, Skill> skillMap = skills.stream()
                .collect(Collectors.toMap(
                        s -> s.getName().toLowerCase(),
                        Function.identity()
                ));

        jobSkillRepository.deleteByJob_Id(job.getId());

        List<JobSkill> jobSkills = new ArrayList<>();
        for (Map.Entry<String, RequirementSectionType> entry : skillLevels.entrySet()) {
            Skill skill = skillMap.get(entry.getKey());
            if (skill == null) {
                continue;
            }
            jobSkills.add(JobSkill.builder()
                    .job(job)
                    .skill(skill)
                    .requirementType(entry.getValue())
                    .confidence(confidenceFor(entry.getValue()))
                    .build());
        }

        jobSkillRepository.saveAll(jobSkills);
    }

    private void addSkills(
            Map<String, RequirementSectionType> target,
            List<String> skills,
            RequirementSectionType level
    ) {
        if (skills == null) {
            return;
        }
        for (String skill : skills) {
            String normalized = normalizeSkill(skill);
            if (normalized == null) {
                continue;
            }
            RequirementSectionType existing = target.get(normalized);
            if (existing == null || priority(level) < priority(existing)) {
                target.put(normalized, level);
            }
        }
    }

    private String normalizeSkill(String skill) {
        if (skill == null) {
            return null;
        }
        String trimmed = skill.trim().toLowerCase();
        return trimmed.isBlank() ? null : trimmed;
    }

    private int priority(RequirementSectionType type) {
        return switch (type) {
            case REQUIRED -> 1;
            case PREFERRED -> 2;
            default -> 3;
        };
    }

    private double confidenceFor(RequirementSectionType type) {
        return switch (type) {
            case REQUIRED -> 0.95;
            case PREFERRED -> 0.85;
            default -> 0.7;
        };
    }
}
