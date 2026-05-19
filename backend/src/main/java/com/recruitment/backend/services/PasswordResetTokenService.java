package com.recruitment.backend.services;

import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RESET_TOKEN_KEY_PREFIX = "auth:reset-token:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.reset-token-ttl-minutes:15}")
    private Integer resetTokenTtlMinutes;

    public String generateToken(String email) {
        String token = generateRawToken();
        String key = buildResetTokenKey(email);
        String hashedToken = passwordEncoder.encode(token);
        stringRedisTemplate.opsForValue().set(key, hashedToken, Duration.ofMinutes(resetTokenTtlMinutes));
        return token;
    }

    public void verifyAndConsumeToken(String email, String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID_OR_EXPIRED);
        }
        String key = buildResetTokenKey(email);
        String hashedToken = stringRedisTemplate.opsForValue().get(key);

        if (hashedToken == null || !passwordEncoder.matches(token, hashedToken)) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID_OR_EXPIRED);
        }

        Boolean deleted = stringRedisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID_OR_EXPIRED);
        }
    }

    private String buildResetTokenKey(String email) {
        return RESET_TOKEN_KEY_PREFIX + email.toLowerCase();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
