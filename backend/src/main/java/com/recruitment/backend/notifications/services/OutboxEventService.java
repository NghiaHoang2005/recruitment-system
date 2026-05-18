package com.recruitment.backend.notifications.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruitment.backend.notifications.domain.entities.OutboxEvent;
import com.recruitment.backend.notifications.domain.enums.OutboxEventStatus;
import com.recruitment.backend.notifications.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void append(String eventType, Object payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .payloadJson(toJson(payload))
                .status(OutboxEventStatus.NEW)
                .build();
        outboxEventRepository.save(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox payload", e);
        }
    }
}
