package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.dtos.JobRequirementItemDTO;
import com.recruitment.backend.domain.dtos.JobRequirementSectionDTO;
import com.recruitment.backend.domain.entities.*;
import com.recruitment.backend.domain.enums.*;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.JobMapper;
import com.recruitment.backend.repositories.*;
import com.recruitment.backend.services.ai.config.AiProperties;
import com.recruitment.backend.services.ai.model.EmbeddingRequest;
import com.recruitment.backend.services.ai.model.EmbeddingResult;
import com.recruitment.backend.services.ai.providers.EmbeddingProvider;
import com.recruitment.backend.services.ai.providers.ProviderRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final CompanyRepository companyRepository;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final ProviderRegistry providerRegistry;
    private final AiProperties aiProperties;
    private final JobMapper jobMapper;

    @Transactional
    public JobDTO createJob(JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Company company = getApprovedCompanyForRecruiter(recruiter);

        if (company.getStatus() == CompanyStatus.REJECTED || company.getStatus() == CompanyStatus.BLOCKED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        JobStatus status = company.getStatus() == CompanyStatus.ACTIVE ? JobStatus.PUBLISHED : JobStatus.PENDING;

        Job job = Job.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .workingTime(dto.getWorkingTime())
                .location(dto.getLocation())
                .employmentType(dto.getEmploymentType())
                .workMode(dto.getWorkMode())
                .level(dto.getLevel())
                .minSalary(dto.getMinSalary())
                .maxSalary(dto.getMaxSalary())
                .currency(dto.getCurrency())
                .salaryNegotiable(Boolean.TRUE.equals(dto.getSalaryNegotiable()))
                .headcount(dto.getHeadcount())
                .deadline(dto.getDeadline())
                .company(company)
                .status(status)
                .publishedAt(status == JobStatus.PUBLISHED ? LocalDateTime.now() : null)
                .recruiter(recruiter)
                .build();

        replaceRequirementSections(job, dto.getRequirementSections());

        Job savedJob = jobRepository.save(job);
        embedJob(savedJob);
        return jobMapper.toDto(savedJob);
    }

    @Transactional
    public JobDTO updateJob(UUID id, JobDTO dto, String userEmail) {
        User recruiter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Company company = getApprovedCompanyForRecruiter(recruiter);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setWorkingTime(dto.getWorkingTime());
        job.setLocation(dto.getLocation());
        job.setEmploymentType(dto.getEmploymentType());
        job.setWorkMode(dto.getWorkMode());
        job.setLevel(dto.getLevel());
        job.setMinSalary(dto.getMinSalary());
        job.setMaxSalary(dto.getMaxSalary());
        job.setCurrency(dto.getCurrency());
        job.setSalaryNegotiable(Boolean.TRUE.equals(dto.getSalaryNegotiable()));
        job.setHeadcount(dto.getHeadcount());
        job.setDeadline(dto.getDeadline());

        replaceRequirementSections(job, dto.getRequirementSections());

        Job savedJob = jobRepository.save(job);
        embedJob(savedJob);
        return jobMapper.toDto(savedJob);
    }

    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll().stream().map(jobMapper::toDto).collect(Collectors.toList());
    }

    public JobDTO getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        return jobMapper.toDto(job);
    }

    private Company getApprovedCompanyForRecruiter(User recruiter) {
        CompanyMember membership = companyMemberRepository.findFirstByUser_IdAndJoinStatus(recruiter.getId(), JoinStatus.APPROVED)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_MEMBER_NOT_FOUND));
        return companyRepository.findById(membership.getCompany().getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    }

    private void replaceRequirementSections(Job job, List<JobRequirementSectionDTO> sectionDtos) {
        job.getRequirementSections().clear();
        if (sectionDtos == null || sectionDtos.isEmpty()) {
            return;
        }

        List<JobRequirementSectionDTO> sortedSections = sectionDtos.stream()
                .sorted(Comparator.comparing(section -> section.getDisplayOrder() == null ? 0 : section.getDisplayOrder()))
                .toList();

        int sectionIndex = 0;
        for (JobRequirementSectionDTO sectionDto : sortedSections) {
            if (sectionDto.getTitle() == null || sectionDto.getTitle().isBlank()) {
                continue;
            }
            JobRequirementSection section = JobRequirementSection.builder()
                    .job(job)
                    .title(sectionDto.getTitle().trim())
                    .sectionType(sectionDto.getSectionType() == null ? RequirementSectionType.OTHER : sectionDto.getSectionType())
                    .displayOrder(sectionDto.getDisplayOrder() == null ? sectionIndex : sectionDto.getDisplayOrder())
                    .build();

            List<JobRequirementItemDTO> itemDtos = sectionDto.getItems() == null ? List.of() : sectionDto.getItems();
            int itemIndex = 0;
            for (JobRequirementItemDTO itemDto : itemDtos) {
                if (itemDto.getContent() == null || itemDto.getContent().isBlank()) {
                    continue;
                }
                JobRequirementItem item = JobRequirementItem.builder()
                        .section(section)
                        .content(itemDto.getContent().trim())
                        .displayOrder(itemDto.getDisplayOrder() == null ? itemIndex : itemDto.getDisplayOrder())
                        .build();
                section.getItems().add(item);
                itemIndex++;
            }

            job.getRequirementSections().add(section);
            sectionIndex++;
        }
    }

    private void embedJob(Job job) {
        String embeddingText = buildEmbeddingText(job);
        if (embeddingText.isBlank()) {
            return;
        }

        try {
            EmbeddingProvider provider = providerRegistry.getEmbeddingProvider();
            Integer dimensions = aiProperties.getEmbedding().getRecommendedDimensions();
            EmbeddingResult result = provider.embed(EmbeddingRequest.builder()
                    .texts(List.of(embeddingText))
                    .model(aiProperties.getEmbedding().getModel())
                    .dimensions(dimensions)
                    .timeoutMs(aiProperties.getEmbedding().getTimeoutMs())
                    .build());

            if (result.getVectors().isEmpty()) {
                log.warn("No job embedding vector returned for job {}", job.getId());
                return;
            }

            jobEmbeddingRepository.deleteByJob_Id(job.getId());
            jobEmbeddingRepository.save(JobEmbedding.builder()
                    .job(job)
                    .embeddingType(JobEmbeddingType.FULL_JOB)
                    .content(embeddingText)
                    .model(result.getModelName())
                    .dimensions(result.getDimensions())
                    .tokenCount(approxTokenCount(embeddingText))
                    .vector(result.getVectors().get(0))
                    .build());
        } catch (Exception ex) {
            log.warn("Could not generate job embedding for job {}: {}", job.getId(), ex.getMessage());
        }
    }

    private String buildEmbeddingText(Job job) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Job title", job.getTitle());
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

    private void appendRequirementGroup(StringBuilder builder, Job job, RequirementSectionType type, String heading) {
        List<JobRequirementSection> sections = job.getRequirementSections() == null ? List.of() : job.getRequirementSections();
        List<String> items = sections.stream()
                .filter(section -> section.getSectionType() == type)
                .flatMap(section -> section.getItems().stream())
                .sorted(Comparator.comparing(item -> item.getDisplayOrder() == null ? 0 : item.getDisplayOrder()))
                .map(JobRequirementItem::getContent)
                .filter(content -> content != null && !content.isBlank())
                .toList();

        if (items.isEmpty()) {
            return;
        }

        builder.append("\n").append(heading).append(":\n");
        items.forEach(item -> builder.append("- ").append(item).append("\n"));
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

    private void appendLine(StringBuilder builder, String label, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append("\n");
    }

    private int approxTokenCount(String text) {
        return Math.max(1, text.length() / 4);
    }
}
