package com.recruitment.backend.notifications.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.domain.entities.OutboxEvent;
import com.recruitment.backend.notifications.domain.enums.OutboxEventStatus;
import com.recruitment.backend.notifications.dto.NotificationEnqueueCommand;
import com.recruitment.backend.notifications.dto.NotificationOutboxPayload;
import com.recruitment.backend.notifications.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxDispatcher {
    public static final String EVENT_NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    @Scheduled(fixedDelayString = "${notification.outbox-dispatch-interval-ms:3000}")
    @Transactional
    public void dispatch() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW,
                PageRequest.of(0, notificationProperties.getOutboxBatchSize())
        );

        for (OutboxEvent event : events) {
            try {
                if (!EVENT_NOTIFICATION_REQUESTED.equals(event.getEventType())) {
                    throw new IllegalStateException("Unsupported event type: " + event.getEventType());
                }

                NotificationOutboxPayload payload = objectMapper.readValue(event.getPayloadJson(), NotificationOutboxPayload.class);
                notificationService.enqueue(NotificationEnqueueCommand.builder()
                        .type(payload.getNotificationType())
                        .recipientEmail(payload.getRecipientEmail())
                        .payload(payload.getPayload())
                        .priority(payload.getPriority())
                        .scheduledAt(payload.getScheduledAt())
                        .idempotencyKey(payload.getIdempotencyKey())
                        .build());

                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setErrorMessage(null);
                event.setPublishedAt(LocalDateTime.now());
            } catch (RuntimeException e) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setErrorMessage(e.getMessage());
                log.error("Failed to dispatch outbox event {}", event.getId(), e);
            } catch (Exception e) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setErrorMessage(e.getMessage());
                log.error("Failed to parse outbox event payload {}", event.getId(), e);
            }
        }
    }
}
