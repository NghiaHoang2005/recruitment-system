package com.recruitment.backend.repositories;

import com.recruitment.backend.domain.entities.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
    @Query("""
            select log from AdminAuditLog log
            left join log.adminUser adminUser
            where (:adminUserId is null or adminUser.id = :adminUserId)
              and (:action is null or log.action = :action)
              and (:targetType is null or log.targetType = :targetType)
              and (:targetId is null or log.targetId = :targetId)
              and (:fromDate is null or log.createdAt >= :fromDate)
              and (:toDate is null or log.createdAt <= :toDate)
            """)
    Page<AdminAuditLog> searchAdminAuditLogs(
            @Param("adminUserId") UUID adminUserId,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("targetId") UUID targetId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
