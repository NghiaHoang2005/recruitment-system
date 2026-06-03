//package com.recruitment.backend.services;
//
//import com.recruitment.backend.domain.entities.Job;
//import com.recruitment.backend.domain.entities.JobRequirementItem;
//import com.recruitment.backend.domain.entities.JobRequirementSection;
//import com.recruitment.backend.domain.entities.JobSkill;
//import com.recruitment.backend.domain.entities.Skill;
//import com.recruitment.backend.domain.enums.RequirementSectionType;
//import com.recruitment.backend.repositories.JobSkillRepository;
//import com.recruitment.backend.repositories.SkillRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class JobSkillExtractorService {
//
//    private final JobSkillRepository jobSkillRepository;
//    private final SkillRepository skillRepository;
//
//    private static final Set<String> COMMON_TECH_SKILLS = Set.of(
//            "java", "python", "javascript", "typescript", "golang", "rust", "kotlin", "scala",
//            "c++", "c#", "php", "ruby", "swift", "objective-c", "dart", "r", "julia",
//            "react", "vue", "angular", "nextjs", "nuxt", "svelte",
//            "spring", "spring boot", "express", "nestjs", "django", "fastapi", "rails", "laravel",
//            "postgresql", "mysql", "mongodb", "redis", "cassandra", "elasticsearch",
//            "docker", "kubernetes", "aws", "azure", "gcp", "heroku",
//            "git", "jenkins", "gitlab", "github", "circleci",
//            "junit", "pytest", "jest", "mocha", "rspec",
//            "rest api", "graphql", "grpc", "kafka", "rabbitmq",
//            "html", "css", "sass", "less", "tailwind", "bootstrap",
//            "linux", "unix", "windows", "macos",
//            "agile", "scrum", "kanban", "devops", "microservices", "cloud",
//            "machine learning", "deep learning", "nlp", "computer vision", "ai",
//            "sql", "nosql", "orm", "jdbc", "hibernate",
//            "soap", "json", "xml", "yaml", "toml",
//            "ci/cd", "tdd", "bdd", "ddd"
//    );
//
//    private static final Set<String> SOFT_SKILLS = Set.of(
//            "communication", "leadership", "teamwork", "collaboration",
//            "problem solving", "critical thinking", "analytical", "creativity",
//            "time management", "organization", "attention to detail",
//            "adaptability", "flexibility", "resilience", "learning",
//            "management", "mentoring", "delegation", "motivation",
//            "presentation", "writing", "negotiation", "conflict resolution"
//    );
//
//    @Transactional
//    public void extractAndLinkSkillsForJob(Job job) {
//        if (job == null || job.getId() == null) {
//            return;
//        }
//
//        try {
//            jobSkillRepository.deleteByJob_Id(job.getId());
//
//            Set<String> extractedSkillNames = new HashSet<>();
//
//            extractedSkillNames.addAll(extractSkillsFromRequirements(job, RequirementSectionType.REQUIRED));
//            extractedSkillNames.addAll(extractSkillsFromRequirements(job, RequirementSectionType.PREFERRED));
//
//            if (job.getDescription() != null && !job.getDescription().isBlank()) {
//                extractedSkillNames.addAll(extractSkillsFromText(job.getDescription(), false));
//            }
//
//            if (job.getTitle() != null && !job.getTitle().isBlank()) {
//                extractedSkillNames.addAll(extractSkillsFromText(job.getTitle(), false));
//            }
//
//            if (extractedSkillNames.isEmpty()) {
//                log.debug("No skills extracted for job {}", job.getId());
//                return;
//            }
//
//            Map<String, JobSkill> skillsToSave = new HashMap<>();
//            for (String skillName : extractedSkillNames) {
//                String normalizedName = skillName.trim().toLowerCase();
//
//                Skill skill = skillRepository.findByNameIgnoreCase(normalizedName)
//                        .orElseGet(() -> {
//                            Skill newSkill = new Skill();
//                            newSkill.setName(normalizedName);
//                            newSkill.setIsVerified(false);
//                            return skillRepository.save(newSkill);
//                        });
//
//                if (!skillsToSave.containsKey(skill.getId().toString())) {
//                    JobSkill jobSkill = JobSkill.builder()
//                            .job(job)
//                            .skill(skill)
//                            .isRequired(false)
//                            .proficiencyLevel(1)
//                            .confidence(0.8)
//                            .source(JobSkill.SkillSource.EXTRACTED)
//                            .build();
//                    skillsToSave.put(skill.getId().toString(), jobSkill);
//                }
//            }
//
//            jobSkillRepository.saveAll(skillsToSave.values());
//            log.info("Extracted {} skills for job {}", skillsToSave.size(), job.getId());
//
//        } catch (Exception e) {
//            log.error("Error extracting skills for job {}: {}", job.getId(), e.getMessage(), e);
//        }
//    }
//
//    private Set<String> extractSkillsFromRequirements(Job job, RequirementSectionType type) {
//        Set<String> skills = new HashSet<>();
//
//        if (job.getRequirementSections() == null) {
//            return skills;
//        }
//
//        job.getRequirementSections().stream()
//                .filter(section -> section.getSectionType() == type)
//                .flatMap(section -> section.getItems().stream())
//                .map(JobRequirementItem::getContent)
//                .filter(content -> content != null && !content.isBlank())
//                .forEach(content -> skills.addAll(extractSkillsFromText(content, true)));
//
//        return skills;
//    }
//
//    private Set<String> extractSkillsFromText(String text, boolean isStructured) {
//        Set<String> skills = new HashSet<>();
//
//        if (text == null || text.isBlank()) {
//            return skills;
//        }
//
//        String lowerText = text.toLowerCase();
//
//        for (String skill : COMMON_TECH_SKILLS) {
//            if (isStructured) {
//                if (lowerText.contains(skill)) {
//                    skills.add(skill);
//                }
//            } else {
//                if (matchSkillInText(lowerText, skill)) {
//                    skills.add(skill);
//                }
//            }
//        }
//
//        for (String skill : SOFT_SKILLS) {
//            if (matchSkillInText(lowerText, skill)) {
//                skills.add(skill);
//            }
//        }
//
//        return skills;
//    }
//
//    private boolean matchSkillInText(String text, String skill) {
//        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b");
//        return pattern.matcher(text).find();
//    }
//
//    public List<JobSkill> getRequiredSkillsForJob(UUID jobId) {
//        return jobSkillRepository.findByJob_IdAndRequirementType(jobId,RequirementSectionType.REQUIRED);
//    }
//
//    public List<JobSkill> getPreferredSkillsForJob(UUID jobId) {
//        return jobSkillRepository.findByJob_IdAndRequirementType(jobId, RequirementSectionType.PREFERRED);
//    }
//
//    public List<String> getSkillNamesForJob(UUID jobId) {
//        return jobSkillRepository.findByJob_Id(jobId).stream()
//                .map(js -> js.getSkill().getName())
//                .collect(Collectors.toList());
//    }
//}
