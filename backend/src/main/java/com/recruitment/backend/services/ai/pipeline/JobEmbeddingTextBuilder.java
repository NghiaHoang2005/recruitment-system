package com.recruitment.backend.services.ai.pipeline;

import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobRequirementItem;
import com.recruitment.backend.domain.entities.JobRequirementSection;
import com.recruitment.backend.domain.entities.JobSkill;
import com.recruitment.backend.domain.enums.RequirementSectionType;
import com.recruitment.backend.repositories.JobSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JobEmbeddingTextBuilder {

    private final JobSkillRepository jobSkillRepository;

    public String buildEmbeddingText(Job job) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Job title", job.getTitle());
        appendLine(builder, "Job categories", formatCategories(job));
        appendLine(builder, "Job description", job.getDescription());
        appendLine(builder, "Working time", job.getWorkingTime());
        appendLine(builder, "Location", job.getLocation());
        appendLine(builder, "Employment type", job.getEmploymentType());
        appendLine(builder, "Work mode", job.getWorkMode());
        appendLine(builder, "Level", job.getLevel());
        appendLine(builder, "Salary", formatSalary(job));

        appendRequirementGroup(builder, job, RequirementSectionType.REQUIRED, "Required skills and experience");
        appendRequirementGroup(builder, job, RequirementSectionType.PREFERRED, "Preferred skills");
        appendRequirementGroup(builder, job, RequirementSectionType.OTHER, "Other requirements");

        return builder.toString().trim();
    }

    public String buildDescriptionText(Job job) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Job title", job.getTitle());
        appendLine(builder, "Job categories", formatCategories(job));
        appendLine(builder, "Job description", job.getDescription());
        appendLine(builder, "Working time", job.getWorkingTime());
        appendLine(builder, "Location", job.getLocation());
        appendLine(builder, "Employment type", job.getEmploymentType());
        appendLine(builder, "Work mode", job.getWorkMode());
        appendLine(builder, "Level", job.getLevel());
        appendLine(builder, "Salary", formatSalary(job));
        return builder.toString().trim();
    }

    public String buildSkillsText(Job job) {
        if (job.getId() != null) {
            List<JobSkill> requiredSkills =
                    jobSkillRepository.findByJob_IdAndRequirementType(job.getId(), RequirementSectionType.REQUIRED);
            List<JobSkill> preferredSkills =
                    jobSkillRepository.findByJob_IdAndRequirementType(job.getId(), RequirementSectionType.PREFERRED);
            if (!requiredSkills.isEmpty() || !preferredSkills.isEmpty()) {
                StringBuilder builder = new StringBuilder("Skills:\n");
                requiredSkills.stream()
                        .map(skill -> skill.getSkill().getName())
                        .forEach(item -> builder.append("- ").append(item).append("\n"));
                preferredSkills.stream()
                        .map(skill -> skill.getSkill().getName())
                        .forEach(item -> builder.append("- ").append(item).append("\n"));
                return builder.toString().trim();
            }
        }

        List<String> required = collectRequirementItems(job, RequirementSectionType.REQUIRED);
        List<String> preferred = collectRequirementItems(job, RequirementSectionType.PREFERRED);
        if (required.isEmpty() && preferred.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("Skills:\n");
        required.forEach(item -> builder.append("- ").append(item).append("\n"));
        preferred.forEach(item -> builder.append("- ").append(item).append("\n"));
        return builder.toString().trim();
    }

    public String buildRequirementText(Job job, RequirementSectionType type, String heading) {
        List<String> items = collectRequirementItems(job, type);
        if (items.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(heading).append(":\n");
        items.forEach(item -> builder.append("- ").append(item).append("\n"));
        return builder.toString().trim();
    }

    public String buildRequirementsTextForExtraction(Job job) {
        StringBuilder builder = new StringBuilder();
        appendRequirementGroup(builder, job, RequirementSectionType.REQUIRED, "Required");
        appendRequirementGroup(builder, job, RequirementSectionType.PREFERRED, "Preferred");
        appendRequirementGroup(builder, job, RequirementSectionType.OTHER, "Other");
        return builder.toString().trim();
    }

    private void appendRequirementGroup(StringBuilder builder, Job job, RequirementSectionType type, String heading) {
        List<String> items = collectRequirementItems(job, type);

        if (items.isEmpty()) {
            return;
        }

        builder.append("\n").append(heading).append(":\n");
        items.forEach(item -> builder.append("- ").append(item).append("\n"));
    }

    private List<String> collectRequirementItems(Job job, RequirementSectionType type) {
        List<JobRequirementSection> sections = job.getRequirementSections() == null ? List.of() : job.getRequirementSections();
        return sections.stream()
                .filter(section -> section.getSectionType() == type)
                .flatMap(section -> section.getItems().stream())
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()))
                .map(JobRequirementItem::getContent)
                .filter(content -> content != null && !content.isBlank())
                .toList();
    }

    private String formatSalary(Job job) {
        if (Boolean.TRUE.equals(job.getSalaryNegotiable())) {
            return "Negotiable";
        }
        if (job.getMinSalary() == null && job.getMaxSalary() == null) {
            return "";
        }
        String currency = job.getCurrency() == null ? "" : " " + job.getCurrency();
        if (job.getMinSalary() != null && job.getMaxSalary() != null) {
            return job.getMinSalary() + " - " + job.getMaxSalary() + currency;
        }
        if (job.getMinSalary() != null) {
            return "From " + job.getMinSalary() + currency;
        }
        return "Up to " + job.getMaxSalary() + currency;
    }

    private String formatCategories(Job job) {
        if (job.getCategories() == null || job.getCategories().isEmpty()) {
            return "";
        }
        return job.getCategories().stream()
                .map(category -> category.getName())
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private void appendLine(StringBuilder builder, String label, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append("\n");
    }
}
