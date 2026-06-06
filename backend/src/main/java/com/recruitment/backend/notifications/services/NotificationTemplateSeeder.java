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
        createIfMissing(NotificationType.COMPANY_VERIFIED, locale,
                "Công ty {{companyName}} đã được xác minh",
                "<p>Hồ sơ công ty <b>{{companyName}}</b> đã được admin xác minh.</p><p>{{reason}}</p>");

        createIfMissing(NotificationType.COMPANY_REJECTED, locale,
                "Công ty {{companyName}} bị từ chối xác minh",
                "<p>Hồ sơ công ty <b>{{companyName}}</b> chưa được duyệt.</p><p>Lý do: {{reason}}</p>");

        createIfMissing(NotificationType.COMPANY_MORE_INFO_REQUESTED, locale,
                "Cần bổ sung thông tin cho {{companyName}}",
                "<p>Admin cần thêm thông tin để xác minh công ty <b>{{companyName}}</b>.</p><p>Ghi chú: {{reason}}</p>");

        createIfMissing(NotificationType.JOB_APPROVED, locale,
                "Tin tuyển dụng {{jobTitle}} đã được duyệt",
                "<p>Tin tuyển dụng <b>{{jobTitle}}</b> của {{companyName}} đã được duyệt và có thể hiển thị trên hệ thống.</p><p>{{reason}}</p>");

        createIfMissing(NotificationType.JOB_REJECTED, locale,
                "Tin tuyển dụng {{jobTitle}} bị từ chối",
                "<p>Tin tuyển dụng <b>{{jobTitle}}</b> của {{companyName}} chưa được duyệt.</p><p>Lý do: {{reason}}</p>");

        createIfMissing(NotificationType.JOB_FLAGGED, locale,
                "Tin tuyển dụng {{jobTitle}} đã bị gắn cờ",
                "<p>Tin tuyển dụng <b>{{jobTitle}}</b> của {{companyName}} đã bị admin gắn cờ để kiểm tra.</p><p>Ghi chú: {{reason}}</p>");

        createIfMissing(NotificationType.JOB_CLOSED, locale,
                "Tin tuyển dụng {{jobTitle}} đã được đóng",
                "<p>Tin tuyển dụng <b>{{jobTitle}}</b> của {{companyName}} đã được admin đóng.</p><p>{{reason}}</p>");

        createIfMissing(NotificationType.ADMIN_COMPANY_REVIEW_REQUESTED, locale,
                "Có công ty mới cần xác minh: {{targetName}}",
                "<p>Công ty <b>{{targetName}}</b> vừa được tạo bởi {{requesterEmail}} và đang chờ admin xác minh.</p>");

        createIfMissing(NotificationType.ADMIN_JOB_REVIEW_REQUESTED, locale,
                "Có tin tuyển dụng mới cần duyệt: {{targetName}}",
                "<p>Tin tuyển dụng <b>{{targetName}}</b> vừa được gửi bởi {{requesterEmail}} và đang chờ admin duyệt.</p>");
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
