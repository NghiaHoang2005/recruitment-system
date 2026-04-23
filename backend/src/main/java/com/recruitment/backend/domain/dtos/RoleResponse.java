package com.recruitment.backend.domain.dtos;

import com.recruitment.backend.domain.entities.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
}
