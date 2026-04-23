package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
}
