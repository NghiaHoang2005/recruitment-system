package com.recruitment.backend.notifications.repositories;

import com.recruitment.backend.notifications.domain.entities.Notification;
import com.recruitment.backend.notifications.domain.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    List<Notification> findByStatusAndScheduledAtLessThanEqualOrderByPriorityDescCreatedAtAsc(
            NotificationStatus status,
            LocalDateTime scheduledAt,
            Pageable pageable
    );
}
