package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDTO {
    private UUID id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String salaryRange;
    private String companyName;
    private LocalDateTime createdAt;
    private UUID recruiterId;
}
