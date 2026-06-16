package com.recruitment.backend.domain.dtos;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCategoryDTO {
    private String code;
    private String name;
}
