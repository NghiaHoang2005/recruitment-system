package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobCategoryDTO;
import com.recruitment.backend.domain.dtos.JobRequirementItemDTO;
import com.recruitment.backend.domain.dtos.JobRequirementSectionDTO;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobRequirementItem;
import com.recruitment.backend.domain.entities.JobRequirementSection;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import com.recruitment.backend.domain.dtos.JobSummaryDTO;

@Component
public class JobMapper {
    public JobSummaryDTO toSummaryDto(Job job) {
        if (job == null) return null;
        return JobSummaryDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType())
                .workMode(job.getWorkMode())
                .level(job.getLevel())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .currency(job.getCurrency())
                .salaryNegotiable(job.getSalaryNegotiable())
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .companyLogoUrl(job.getCompany() != null ? job.getCompany().getLogoUrl() : null)
                .companyIndustry(job.getCompany() != null ? job.getCompany().getIndustry() : null)
                .companySize(job.getCompany() != null ? job.getCompany().getCompanySize() : null)
                .companyAddress(job.getCompany() != null ? job.getCompany().getAddress() : null)
                .companyCity(job.getCompany() != null ? job.getCompany().getCity() : null)
                .companyCountry(job.getCompany() != null ? job.getCompany().getCountry() : null)
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .publishedAt(job.getPublishedAt())
                .categories(job.getCategories() == null ? List.of() : job.getCategories().stream()
                        .map(category -> JobCategoryDTO.builder()
                                .code(category.getCode())
                                .name(category.getName())
                                .build())
                        .toList())
                .build();
    }

    public JobDTO toDto(Job job) {
        if (job == null) return null;
        return JobDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .workingTime(job.getWorkingTime())
                .location(job.getLocation())
                .locationCode(job.getStandardLocation() != null ? job.getStandardLocation().getCode() : null)
                .locationName(job.getStandardLocation() != null ? job.getStandardLocation().getName() : null)
                .employmentType(job.getEmploymentType())
                .workMode(job.getWorkMode())
                .level(job.getLevel())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .currency(job.getCurrency())
                .salaryNegotiable(job.getSalaryNegotiable())
                .headcount(job.getHeadcount())
                .deadline(job.getDeadline())
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getName() : null)
                .companyLogoUrl(job.getCompany() != null ? job.getCompany().getLogoUrl() : null)
                .companyIndustry(job.getCompany() != null ? job.getCompany().getIndustry() : null)
                .companySize(job.getCompany() != null ? job.getCompany().getCompanySize() : null)
                .companyAddress(job.getCompany() != null ? job.getCompany().getAddress() : null)
                .companyCity(job.getCompany() != null ? job.getCompany().getCity() : null)
                .companyCountry(job.getCompany() != null ? job.getCompany().getCountry() : null)
                .companyWebsite(job.getCompany() != null ? job.getCompany().getWebsite() : null)
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .publishedAt(job.getPublishedAt())
                .closedAt(job.getClosedAt())
                .recruiterId(job.getRecruiter() != null ? job.getRecruiter().getId() : null)
                .categories(job.getCategories() == null ? List.of() : job.getCategories().stream()
                        .map(category -> JobCategoryDTO.builder()
                                .code(category.getCode())
                                .name(category.getName())
                                .build())
                        .toList())
                .requirementSections(mapSections(job.getRequirementSections()))
                .build();
    }

    private List<JobRequirementSectionDTO> mapSections(List<JobRequirementSection> sections) {
        if (sections == null) return List.of();
        return sections.stream()
                .sorted(Comparator.comparing(section -> section.getDisplayOrder() == null ? 0 : section.getDisplayOrder()))
                .map(this::mapSection)
                .toList();
    }

    private JobRequirementSectionDTO mapSection(JobRequirementSection section) {
        return JobRequirementSectionDTO.builder()
                .id(section.getId())
                .title(section.getTitle())
                .sectionType(section.getSectionType())
                .displayOrder(section.getDisplayOrder())
                .items(mapItems(section.getItems()))
                .build();
    }

    private List<JobRequirementItemDTO> mapItems(List<JobRequirementItem> items) {
        if (items == null) return List.of();
        return items.stream()
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()))
                .map(item -> JobRequirementItemDTO.builder()
                        .id(item.getId())
                        .content(item.getContent())
                        .displayOrder(item.getDisplayOrder())
                        .build())
                .toList();
    }
}
