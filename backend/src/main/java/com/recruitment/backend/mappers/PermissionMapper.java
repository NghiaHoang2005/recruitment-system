package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.PermissionRequest;
import com.recruitment.backend.domain.dtos.PermissionResponse;
import com.recruitment.backend.domain.entities.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
