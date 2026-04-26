package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.JobDTO;
import com.recruitment.backend.domain.entities.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {
    public JobDTO toDto(Job job) {
        if (job == null) return null;
        return JobDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
//                .companyName(job.getCompanyName())
                .createdAt(job.getCreatedAt())
                .recruiterId(job.getRecruiter().getId())
                .build();
    }
}
