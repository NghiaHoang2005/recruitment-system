package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.enums.RequirementSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequirementSectionDTO {
    private UUID id;
    private String title;
    private RequirementSectionType sectionType;
    private Integer displayOrder;
    private List<JobRequirementItemDTO> items;
}
