package com.recruitment.backend.domain.dtos;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionRequest {
    private String name;
    private String description;
}
