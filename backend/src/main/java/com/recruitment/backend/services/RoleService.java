package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.RoleRequest;
import com.recruitment.backend.domain.dtos.RoleResponse;
import com.recruitment.backend.domain.entities.Role;
import com.recruitment.backend.mappers.RoleMapper;
import com.recruitment.backend.repositories.PermissionRepository;
import com.recruitment.backend.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final PermissionRepository permissionRepository;

    public RoleResponse create(RoleRequest request){
        var role = roleMapper.toRole(request);
        Set<String> permissionNames = request.getPermissions() == null ? Set.of() : request.getPermissions();
        var permissions = permissionRepository.findAllById(permissionNames);
        role.setPermissions(new HashSet<>(permissions));
        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll(){
        var roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toRoleResponse).toList();
    }

    public void delete(String role){
        roleRepository.deleteById(role);
    }
}
