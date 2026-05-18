package com.recruitment.backend.notifications.repositories;

import com.recruitment.backend.notifications.domain.entities.NotificationTemplate;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByTypeAndLocaleAndIsActiveTrue(NotificationType type, String locale);

    boolean existsByTypeAndLocale(NotificationType type, String locale);
}
