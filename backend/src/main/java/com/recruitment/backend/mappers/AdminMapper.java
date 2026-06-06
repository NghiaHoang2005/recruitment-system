package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.AdminCompanyResponse;
import com.recruitment.backend.domain.dtos.AdminAuditLogResponse;
import com.recruitment.backend.domain.dtos.AdminDashboardResponse;
import com.recruitment.backend.domain.dtos.AdminJobResponse;
import com.recruitment.backend.domain.dtos.AdminUserResponse;
import com.recruitment.backend.domain.dtos.JobRequirementItemDTO;
import com.recruitment.backend.domain.dtos.JobRequirementSectionDTO;
import com.recruitment.backend.domain.entities.AdminAuditLog;
import com.recruitment.backend.domain.entities.Application;
import com.recruitment.backend.domain.entities.Company;
import com.recruitment.backend.domain.entities.Job;
import com.recruitment.backend.domain.entities.JobRequirementItem;
import com.recruitment.backend.domain.entities.JobRequirementSection;
import com.recruitment.backend.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AdminMapper {
    public AdminUserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public AdminCompanyResponse toCompanyResponse(Company company) {
        if (company == null) {
            return null;
        }

        return AdminCompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .website(company.getWebsite())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .country(company.getCountry())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .taxCode(company.getTaxCode())
                .businessLicense(company.getBusinessLicense())
                .status(company.getStatus())
                .createdById(company.getCreatedBy() != null ? company.getCreatedBy().getId() : null)
                .ownerEmail(company.getCreatedBy() != null ? company.getCreatedBy().getEmail() : null)
                .build();
    }

    public AdminDashboardResponse.CompanyQueueItem toCompanyQueueItem(Company company) {
        if (company == null) {
            return null;
        }

        return AdminDashboardResponse.CompanyQueueItem.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .website(company.getWebsite())
                .industry(company.getIndustry())
                .status(company.getStatus())
                .ownerEmail(company.getCreatedBy() != null ? company.getCreatedBy().getEmail() : null)
                .build();
    }

    public AdminDashboardResponse.JobQueueItem toJobQueueItem(Job job) {
        if (job == null) {
            return null;
        }
        Company company = job.getCompany();

        return AdminDashboardResponse.JobQueueItem.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .status(job.getStatus())
                .location(job.getLocation())
                .createdAt(job.getCreatedAt())
                .publishedAt(job.getPublishedAt())
                .build();
    }

    public AdminDashboardResponse.ApplicationActivityItem toApplicationActivityItem(Application application) {
        if (application == null) {
            return null;
        }
        Job job = application.getJob();
        Company company = job != null ? job.getCompany() : null;

        return AdminDashboardResponse.ApplicationActivityItem.builder()
                .id(application.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyName(company != null ? company.getName() : null)
                .candidateName(application.getCandidate() != null ? application.getCandidate().getFullName() : null)
                .candidateEmail(application.getCandidate() != null && application.getCandidate().getUser() != null
                        ? application.getCandidate().getUser().getEmail()
                        : null)
                .aiScore(application.getAiScore())
                .appliedAt(application.getAppliedAt())
                .build();
    }

    public AdminJobResponse toJobResponse(Job job, long applicationCount) {
        if (job == null) {
            return null;
        }
        Company company = job.getCompany();
        User recruiter = job.getRecruiter();

        return AdminJobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .workingTime(job.getWorkingTime())
                .location(job.getLocation())
                .employmentType(job.getEmploymentType())
                .workMode(job.getWorkMode())
                .level(job.getLevel())
                .minSalary(job.getMinSalary())
                .maxSalary(job.getMaxSalary())
                .currency(job.getCurrency())
                .salaryNegotiable(job.getSalaryNegotiable())
                .headcount(job.getHeadcount())
                .deadline(job.getDeadline())
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .companyStatus(company != null && company.getStatus() != null ? company.getStatus().name() : null)
                .recruiterId(recruiter != null ? recruiter.getId() : null)
                .recruiterEmail(recruiter != null ? recruiter.getEmail() : null)
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .publishedAt(job.getPublishedAt())
                .closedAt(job.getClosedAt())
                .applicationCount(applicationCount)
                .requirementSections(mapSections(job.getRequirementSections()))
                .build();
    }

    public AdminAuditLogResponse toAuditLogResponse(AdminAuditLog log) {
        if (log == null) {
            return null;
        }
        User adminUser = log.getAdminUser();

        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .adminUserId(adminUser != null ? adminUser.getId() : null)
                .adminEmail(adminUser != null ? adminUser.getEmail() : null)
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .reason(log.getReason())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private List<JobRequirementSectionDTO> mapSections(List<JobRequirementSection> sections) {
        if (sections == null) {
            return List.of();
        }
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
        if (items == null) {
            return List.of();
        }
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
