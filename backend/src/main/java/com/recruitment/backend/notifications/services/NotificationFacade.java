package com.recruitment.backend.notifications.services;

import com.recruitment.backend.notifications.dto.NotificationOutboxPayload;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationFacade {
    private final NotificationService notificationService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void requestOtp(String email, String otpCode, Integer ttlMinutes, String idempotencyKey) {
        int finalTtl = ttlMinutes == null ? notificationService.defaultOtpTtlMinutes() : ttlMinutes;
        appendOutbox(NotificationType.OTP, email, Map.of(
                "otpCode", otpCode,
                "ttlMinutes", finalTtl
        ), 100, idempotencyKey);
    }

    @Transactional
    public void notifyApplicationSubmitted(String email, String candidateName, String jobTitle, String idempotencyKey) {
        appendOutbox(NotificationType.APPLICATION_SUBMITTED, email, Map.of(
                "candidateName", candidateName,
                "jobTitle", jobTitle
        ), 50, idempotencyKey);
    }

    @Transactional
    public void notifyApplicationResult(String email,
                                        String candidateName,
                                        String jobTitle,
                                        Boolean passed,
                                        String feedback,
                                        String idempotencyKey) {
        String decision = Boolean.TRUE.equals(passed) ? "ĐẬU" : "RỚT";
        appendOutbox(NotificationType.APPLICATION_RESULT, email, Map.of(
                "candidateName", candidateName,
                "jobTitle", jobTitle,
                "decision", decision,
                "feedback", feedback == null ? "" : feedback
        ), 50, idempotencyKey);
    }

    @Transactional
    public void notifyJobMatch(String email, String candidateName, List<String> matchedJobs, String idempotencyKey) {
        String jobs = String.join("<br/>- ", matchedJobs);
        appendOutbox(NotificationType.JOB_MATCH, email, Map.of(
                "candidateName", candidateName,
                "matchedJobs", "- " + jobs
        ), 30, idempotencyKey);
    }

    @Transactional
    public void notifyUserRegistered(String email, String accountType) {
        appendOutbox(NotificationType.USER_REGISTERED, email, Map.of(
                "email", email,
                "accountType", accountType
        ), 20, "user-registered:" + UUID.nameUUIDFromBytes((email + ":" + accountType).getBytes()));
    }

    private void appendOutbox(NotificationType notificationType,
                              String recipientEmail,
                              Map<String, Object> payload,
                              Integer priority,
                              String idempotencyKey) {
        outboxEventService.append(
                NotificationOutboxDispatcher.EVENT_NOTIFICATION_REQUESTED,
                NotificationOutboxPayload.builder()
                        .notificationType(notificationType)
                        .recipientEmail(recipientEmail)
                        .payload(payload)
                        .priority(priority)
                        .scheduledAt(LocalDateTime.now())
                        .idempotencyKey(idempotencyKey)
                        .build()
        );
    }
}
