package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.PermissionRequest;
import com.recruitment.backend.domain.dtos.PermissionResponse;
import com.recruitment.backend.domain.entities.Permission;
import com.recruitment.backend.mappers.PermissionMapper;
import com.recruitment.backend.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @PreAuthorize("hasRole('ADMIN')")
    public PermissionResponse create(PermissionRequest request){
        Permission permission = permissionMapper.toPermission(request);
        permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<PermissionResponse> getAll(){
        var permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(String permission){
        permissionRepository.deleteById(permission);
    }
}
