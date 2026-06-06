package com.recruitment.backend.services;

import com.recruitment.backend.domain.dtos.AdminAuditLogResponse;
import com.recruitment.backend.domain.dtos.AdminPageResponse;
import com.recruitment.backend.domain.entities.AdminAuditLog;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.mappers.AdminMapper;
import com.recruitment.backend.repositories.AdminAuditLogRepository;
import com.recruitment.backend.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AdminMapper adminMapper;
    private final SecurityUtil securityUtil;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String targetType, UUID targetId, String reason) {
        User adminUser = securityUtil.getCurrentUser();
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .adminUser(adminUser)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(normalize(reason))
                .build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminAuditLogResponse> getAuditLogs(
            int page,
            int size,
            UUID adminUserId,
            String action,
            String targetType,
            UUID targetId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<AdminAuditLog> logs = adminAuditLogRepository.searchAdminAuditLogs(
                adminUserId,
                normalize(action),
                normalize(targetType),
                targetId,
                fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.atTime(LocalTime.MAX) : null,
                pageable
        );

        return AdminPageResponse.<AdminAuditLogResponse>builder()
                .items(logs.stream().map(adminMapper::toAuditLogResponse).toList())
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalItems(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
