package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminPageResponse;
import com.recruitment.backend.domain.dtos.AdminUserResponse;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.mappers.AdminMapper;
import com.recruitment.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;
    private final AdminAuditLogService adminAuditLogService;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminUserResponse> getUsers(
            int page,
            int size,
            String keyword,
            String role,
            Boolean enabled
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<User> users = userRepository.findAll(buildUserSpecification(normalize(keyword), normalizeRole(role), enabled), pageable);

        return AdminPageResponse.<AdminUserResponse>builder()
                .items(users.stream().map(adminMapper::toUserResponse).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalItems(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        return adminMapper.toUserResponse(findUser(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse disableUser(UUID userId, String reason) {
        User user = findUser(userId);
        user.setEnabled(false);
        User savedUser = userRepository.save(user);
        adminAuditLogService.record("USER_DISABLED", "USER", userId, reason);
        return adminMapper.toUserResponse(savedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserResponse enableUser(UUID userId, String reason) {
        User user = findUser(userId);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        adminAuditLogService.record("USER_ENABLED", "USER", userId, reason);
        return adminMapper.toUserResponse(savedUser);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRole(String role) {
        String normalized = normalize(role);
        if (normalized == null || normalized.equalsIgnoreCase("ALL")) {
            return null;
        }
        return normalized.toUpperCase();
    }

    private Specification<User> buildUserSpecification(String keyword, String role, Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role").get("name"), role));
            }
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
