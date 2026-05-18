package com.recruitment.backend.notifications.dto;

import com.recruitment.backend.notifications.domain.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEnqueueCommand {
    private NotificationType type;
    private String recipientEmail;
    private Map<String, Object> payload;
    private Integer priority;
    private LocalDateTime scheduledAt;
    private String idempotencyKey;
}
