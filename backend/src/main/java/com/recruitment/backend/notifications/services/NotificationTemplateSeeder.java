package com.recruitment.backend.notifications.services;

import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.domain.entities.NotificationTemplate;
import com.recruitment.backend.notifications.domain.enums.NotificationType;
import com.recruitment.backend.notifications.repositories.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationTemplateSeeder implements CommandLineRunner {
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationProperties notificationProperties;

    @Override
    public void run(String... args) {
        String locale = notificationProperties.getLocale();
        createIfMissing(NotificationType.OTP, locale,
                "Mã OTP xác thực của bạn",
                "<p>Xin chào,</p><p>Mã OTP của bạn là <b>{{otpCode}}</b>. Mã có hiệu lực trong {{ttlMinutes}} phút.</p>");

        createIfMissing(NotificationType.APPLICATION_SUBMITTED, locale,
                "Ứng tuyển thành công: {{jobTitle}}",
                "<p>Chào {{candidateName}},</p><p>Bạn đã nộp ứng tuyển thành công cho vị trí <b>{{jobTitle}}</b>.</p>");

        createIfMissing(NotificationType.APPLICATION_RESULT, locale,
                "Kết quả ứng tuyển: {{jobTitle}}",
                "<p>Chào {{candidateName}},</p><p>Kết quả ứng tuyển của bạn cho vị trí <b>{{jobTitle}}</b>: <b>{{decision}}</b>.</p><p>{{feedback}}</p>");

        createIfMissing(NotificationType.JOB_MATCH, locale,
                "Gợi ý việc làm phù hợp",
                "<p>Chào {{candidateName}},</p><p>Dưới đây là các công việc phù hợp với bạn:</p><p>{{matchedJobs}}</p>");

        createIfMissing(NotificationType.USER_REGISTERED, locale,
                "Chào mừng bạn đến với Recruitment System",
                "<p>Tài khoản {{accountType}} với email <b>{{email}}</b> đã được tạo thành công.</p>");
    }

    private void createIfMissing(NotificationType type, String locale, String subject, String body) {
        if (notificationTemplateRepository.existsByTypeAndLocale(type, locale)) {
            return;
        }
        notificationTemplateRepository.save(NotificationTemplate.builder()
                .type(type)
                .locale(locale)
                .subjectTemplate(subject)
                .bodyTemplate(body)
                .version(1)
                .isActive(true)
                .build());
    }
}
