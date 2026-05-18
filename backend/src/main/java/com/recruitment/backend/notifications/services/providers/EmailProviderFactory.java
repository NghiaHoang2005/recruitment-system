package com.recruitment.backend.notifications.services.providers;

import com.recruitment.backend.notifications.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmailProviderFactory {
    private final NotificationProperties notificationProperties;
    private final List<EmailProvider> providers;

    public EmailProvider getProvider() {
        Map<String, EmailProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(EmailProvider::providerName, Function.identity()));

        String configuredProvider = notificationProperties.getMail().isSmtpEnabled()
                ? "smtp"
                : notificationProperties.getMail().getProvider();

        return providerMap.getOrDefault(configuredProvider, providerMap.get("logging"));
    }
}
