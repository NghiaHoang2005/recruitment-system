package com.recruitment.backend.notifications.services.providers;

import com.recruitment.backend.notifications.dto.EmailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.mail.MessagingException;

@Slf4j
@Component
public class LoggingEmailProvider implements EmailProvider {
    @Override
    public String providerName() {
        return "logging";
    }

    @Override
    public void send(EmailMessage message) throws MessagingException {
        log.info("Simulated email send | to={} | subject={} | body={}", message.getTo(), message.getSubject(), message.getBody());
    }
}
