package com.recruitment.backend.notifications.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.domain.entities.Notification;
import com.recruitment.backend.notifications.domain.entities.NotificationLog;
import com.recruitment.backend.notifications.domain.entities.NotificationTemplate;
import com.recruitment.backend.notifications.domain.enums.NotificationLogResult;
import com.recruitment.backend.notifications.domain.enums.NotificationStatus;
import com.recruitment.backend.notifications.dto.EmailMessage;
import com.recruitment.backend.notifications.dto.RenderedTemplate;
import com.recruitment.backend.notifications.repositories.NotificationLogRepository;
import com.recruitment.backend.notifications.repositories.NotificationRepository;
import com.recruitment.backend.notifications.repositories.NotificationTemplateRepository;
import com.recruitment.backend.notifications.services.providers.EmailProvider;
import com.recruitment.backend.notifications.services.providers.EmailProviderFactory;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendProcessor {
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationTemplateRenderer templateRenderer;
    private final EmailProviderFactory emailProviderFactory;
    private final NotificationProperties notificationProperties;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || notification.getStatus() != NotificationStatus.PENDING) {
            return;
        }

        EmailProvider provider = emailProviderFactory.getProvider();
        Integer nextAttempt = notification.getAttemptCount() + 1;

        notification.setStatus(NotificationStatus.PROCESSING);
        notification.setProvider(provider.providerName());

        try {
            NotificationTemplate template = notificationTemplateRepository
                    .findByTypeAndLocaleAndIsActiveTrue(notification.getType(), notificationProperties.getLocale())
                    .orElseThrow(() -> new IllegalStateException("Template not found for type=" + notification.getType()));

            Map<String, Object> payload = readPayload(notification.getPayloadJson());
            RenderedTemplate renderedTemplate = templateRenderer.render(template.getSubjectTemplate(), template.getBodyTemplate(), payload);

            provider.send(EmailMessage.builder()
                    .to(notification.getRecipientEmail())
                    .subject(renderedTemplate.getSubject())
                    .body(renderedTemplate.getBody())
                    .build());

            notification.setStatus(NotificationStatus.SENT);
            notification.setAttemptCount(nextAttempt);
            notification.setLastError(null);
            notificationLogRepository.save(NotificationLog.builder()
                    .notification(notification)
                    .provider(provider.providerName())
                    .attempt(nextAttempt)
                    .result(NotificationLogResult.SUCCESS)
                    .build());
        } catch (MessagingException | RuntimeException e) {
            notification.setAttemptCount(nextAttempt);
            notification.setLastError(e.getMessage());

            if (nextAttempt >= notificationProperties.getMaxAttempts()) {
                notification.setStatus(NotificationStatus.FAILED);
            } else {
                notification.setStatus(NotificationStatus.PENDING);
                notification.setScheduledAt(LocalDateTime.now().plusMinutes(1));
            }

            notificationLogRepository.save(NotificationLog.builder()
                    .notification(notification)
                    .provider(provider.providerName())
                    .attempt(nextAttempt)
                    .result(NotificationLogResult.FAILED)
                    .errorMessage(e.getMessage())
                    .build());
            log.error("Failed to send notification {}", notification.getId(), e);
        }
    }

    private Map<String, Object> readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse notification payload", e);
        }
    }
}
