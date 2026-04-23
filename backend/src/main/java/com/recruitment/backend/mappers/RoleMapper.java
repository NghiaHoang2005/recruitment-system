package com.recruitment.backend.mappers;

import com.recruitment.backend.domain.dtos.RoleRequest;
import com.recruitment.backend.domain.dtos.RoleResponse;
import com.recruitment.backend.domain.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);
    RoleResponse toRoleResponse(Role role);
}
