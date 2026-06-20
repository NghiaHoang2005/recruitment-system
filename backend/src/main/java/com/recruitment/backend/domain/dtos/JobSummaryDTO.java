package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.EmploymentType;
import com.recruitment.backend.domain.enums.JobLevel;
import com.recruitment.backend.domain.enums.JobStatus;
import com.recruitment.backend.domain.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobSummaryDTO {
    private UUID id;
    private String title;
    private String location;
    private EmploymentType employmentType;
    private WorkMode workMode;
    private JobLevel level;
    private Integer minSalary;
    private Integer maxSalary;
    private String currency;
    private Boolean salaryNegotiable;
    private UUID companyId;
    private String companyName;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private List<JobCategoryDTO> categories;
}
