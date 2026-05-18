package com.recruitment.backend.notifications.services.providers;

import com.recruitment.backend.notifications.dto.EmailMessage;
import jakarta.mail.MessagingException;

public interface EmailProvider {
    String providerName();

    void send(EmailMessage message) throws MessagingException;
}
