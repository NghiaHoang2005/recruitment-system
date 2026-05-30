package com.recruitment.backend.domain.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditMetadata {
    private UUID adminUserId;
    private String action;
    private String targetType;
    private UUID targetId;
    private String reason;
    private LocalDateTime createdAt;
}
