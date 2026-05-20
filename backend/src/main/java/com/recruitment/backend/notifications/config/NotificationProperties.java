package com.recruitment.backend.notifications.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {
    private String locale = "vi";
    private Integer maxAttempts = 3;
    private Integer otpTtlMinutes = 5;
    private Integer outboxBatchSize = 50;
    private Integer sendBatchSize = 50;
    private String fromAddress = "hoangquangnghia123@gmail.com";
    private String fromName = "Recruitment Curator";
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Mail {
        private boolean smtpEnabled = true;
        private String provider = "logging";
    }
}
