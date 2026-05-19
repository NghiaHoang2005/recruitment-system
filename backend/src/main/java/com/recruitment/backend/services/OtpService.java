package com.recruitment.backend.services;

import com.recruitment.backend.domain.enums.OtpPurpose;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.notifications.services.NotificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_KEY_PREFIX = "auth:otp:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final NotificationFacade notificationFacade;

    @Value("${notification.otp-ttl-minutes:5}")
    private Integer otpTtlMinutes;

    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = buildOtpKey(email, purpose);
        String hashedCode = passwordEncoder.encode(otpCode);

        stringRedisTemplate.opsForValue().set(key, hashedCode, Duration.ofMinutes(otpTtlMinutes));

        String idempotencyKey = "otp:" + purpose.name() + ":" + UUID.randomUUID();
        notificationFacade.requestOtp(email, otpCode, otpTtlMinutes, idempotencyKey);
    }

    public void verifyAndConsumeOtp(String email, OtpPurpose purpose, String otpCode) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }
        String key = buildOtpKey(email, purpose);
        String hashedCode = stringRedisTemplate.opsForValue().get(key);

        if (hashedCode == null || !passwordEncoder.matches(otpCode, hashedCode)) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }

        Boolean deleted = stringRedisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }
    }

    private String buildOtpKey(String email, OtpPurpose purpose) {
        return OTP_KEY_PREFIX + purpose.name() + ":" + email.toLowerCase();
    }
}
