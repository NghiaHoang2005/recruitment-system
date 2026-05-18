package com.recruitment.backend.notifications.repositories;

import com.recruitment.backend.notifications.domain.entities.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
}
