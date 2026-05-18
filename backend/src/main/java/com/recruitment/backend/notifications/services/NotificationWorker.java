package com.recruitment.backend.notifications.services;

import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.domain.entities.Notification;
import com.recruitment.backend.notifications.domain.enums.NotificationStatus;
import com.recruitment.backend.notifications.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationWorker {
    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;
    private final NotificationSendProcessor notificationSendProcessor;

    @Scheduled(fixedDelayString = "${notification.worker-interval-ms:5000}")
    @Transactional(readOnly = true)
    public void processPendingNotifications() {
        List<Notification> readyNotifications = notificationRepository.findByStatusAndScheduledAtLessThanEqualOrderByPriorityDescCreatedAtAsc(
                NotificationStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, notificationProperties.getSendBatchSize())
        );
        for (Notification notification : readyNotifications) {
            notificationSendProcessor.processNotification(notification.getId());
        }
    }
}
