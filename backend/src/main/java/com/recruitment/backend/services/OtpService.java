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
    private static final String OTP_RESEND_COOLDOWN_KEY_PREFIX = "auth:otp:resend:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final NotificationFacade notificationFacade;

    @Value("${notification.otp-ttl-minutes:5}")
    private Integer otpTtlMinutes;

    @Value("${auth.otp-resend-cooldown-seconds:60}")
    private Integer otpResendCooldownSeconds;

    public int getOtpTtlSeconds() {
        return otpTtlMinutes * 60;
    }
    public int getOtpResendCooldownSeconds() {
        return otpResendCooldownSeconds;
    }
    private void checkAndSetCooldown(String email, OtpPurpose purpose) {
        String cooldownKey = buildResendCooldownKey(email, purpose);

        Boolean allowed = stringRedisTemplate.opsForValue()
                .setIfAbsent(
                        cooldownKey,
                        "1",
                        Duration.ofSeconds(otpResendCooldownSeconds)
                );

        if (Boolean.FALSE.equals(allowed)) {
            throw new AppException(ErrorCode.OTP_TOO_MANY_REQUESTS);
        }
    }
    private void generateAndSendOtp(String email, OtpPurpose purpose) {
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        String key = buildOtpKey(email, purpose);
        String hashedCode = passwordEncoder.encode(otpCode);

        stringRedisTemplate.opsForValue().set(key, hashedCode, Duration.ofMinutes(otpTtlMinutes));

        String idempotencyKey = "otp:" + purpose.name() + ":" + UUID.randomUUID();
        notificationFacade.requestOtp(email, otpCode, otpTtlMinutes, idempotencyKey);
    }

    public void requestOtp(String email, OtpPurpose purpose) {
        checkAndSetCooldown(email, purpose);
        generateAndSendOtp(email, purpose);
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
        if (!deleted) {
            throw new AppException(ErrorCode.OTP_INVALID_OR_EXPIRED);
        }
    }

    private String buildOtpKey(String email, OtpPurpose purpose) {
        return OTP_KEY_PREFIX + purpose.name() + ":" + email.toLowerCase();
    }

    private String buildResendCooldownKey(String email, OtpPurpose purpose) {
        return OTP_RESEND_COOLDOWN_KEY_PREFIX + purpose.name() + ":" + email.toLowerCase();
    }
}
