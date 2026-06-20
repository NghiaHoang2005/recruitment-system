package com.recruitment.backend.notifications.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.domain.entities.Notification;
import com.recruitment.backend.notifications.domain.enums.NotificationStatus;
import com.recruitment.backend.notifications.dto.NotificationEnqueueCommand;
import com.recruitment.backend.notifications.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationProperties notificationProperties;

    @Transactional
    public Notification enqueue(NotificationEnqueueCommand command) {
        if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
            Optional<Notification> existing = notificationRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Notification notification = Notification.builder()
                .type(command.getType())
                .recipientEmail(command.getRecipientEmail())
                .payloadJson(toJson(command.getPayload()))
                .status(NotificationStatus.PENDING)
                .priority(command.getPriority() == null ? 0 : command.getPriority())
                .scheduledAt(command.getScheduledAt() == null ? LocalDateTime.now() : command.getScheduledAt())
                .idempotencyKey(command.getIdempotencyKey())
                .attemptCount(0)
                .lastError(null)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    public int defaultOtpTtlMinutes() {
        return notificationProperties.getOtpTtlMinutes();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize notification payload", e);
        }
    }
}
