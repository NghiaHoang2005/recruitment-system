package com.recruitment.backend.notifications.services.providers;

import com.recruitment.backend.notifications.config.NotificationProperties;
import com.recruitment.backend.notifications.dto.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.mail", name = "smtp-enabled", havingValue = "true")
public class SmtpEmailProvider implements EmailProvider {
    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Override
    public String providerName() {
        return "smtp";
    }

    @Override
    public void send(EmailMessage message) throws MessagingException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(new InternetAddress(
                    notificationProperties.getFromAddress(),
                    notificationProperties.getFromName()
            ));
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(message.getBody(), true);
            mailSender.send(mimeMessage);
            log.info("Email sent via SMTP | to={} | subject={}", message.getTo(), message.getSubject());

        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Invalid sender encoding", e);
        }
    }
}
