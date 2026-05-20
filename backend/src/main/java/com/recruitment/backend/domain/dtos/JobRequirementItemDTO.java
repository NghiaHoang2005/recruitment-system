package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobRequirementItemDTO {
    private UUID id;
    private String content;
    private Integer displayOrder;
}
