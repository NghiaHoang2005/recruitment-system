package com.recruitment.backend.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.entities.InvalidatedToken;
import com.recruitment.backend.domain.entities.Role;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.domain.enums.AccountType;
import com.recruitment.backend.domain.enums.OtpPurpose;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.notifications.services.NotificationFacade;
import com.recruitment.backend.repositories.InvalidatedTokenRepository;
import com.recruitment.backend.repositories.RoleRepository;
import com.recruitment.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    @Value("${jwt.signerKey}")
    private String signerKey;

    @Value("${jwt.valid-duration}")
    private Long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    private Long REFRESHABLE_DURATION;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final NotificationFacade notificationFacade;
    private final OtpService otpService;
    private final PasswordResetTokenService passwordResetTokenService;

    public OtpSentResponse requestRegisterOtp(RegisterOtpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        otpService.requestOtp(request.getEmail(), OtpPurpose.REGISTER_VERIFICATION);
        return OtpSentResponse.builder()
                .resendCooldownSeconds(otpService.getOtpResendCooldownSeconds())
                .ttlSeconds(otpService.getOtpTtlSeconds())
                .build();
    }

    public OtpSentResponse requestForgotPasswordOtp(ForgotPasswordOtpRequest request) {
        userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        otpService.requestOtp(request.getEmail(), OtpPurpose.PASSWORD_RESET);
        return OtpSentResponse.builder()
                .resendCooldownSeconds(otpService.getOtpResendCooldownSeconds())
                .ttlSeconds(otpService.getOtpTtlSeconds())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, AccountType accountType) {
        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new AppException(ErrorCode.OTP_REQUIRED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        otpService.verifyAndConsumeOtp(request.getEmail(), OtpPurpose.REGISTER_VERIFICATION, request.getOtpCode());
        Role role = roleRepository.findById(accountType.name()).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        userRepository.save(user);
        notificationFacade.notifyUserRegistered(user.getEmail(), accountType.name());
        var jwtToken = generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .authenticated(true)
                .accountType(accountType.name())
                .userId(user.getId().toString())
                .build();
    }

    public String verifyForgotPasswordOtp(VerifyForgotPasswordOtpRequest request) {
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            throw new AppException(ErrorCode.OTP_REQUIRED);
        }
        userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        otpService.verifyAndConsumeOtp(request.getEmail(), OtpPurpose.PASSWORD_RESET, request.getOtp());
        return passwordResetTokenService.generateToken(request.getEmail());
    }
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        passwordResetTokenService.verifyAndConsumeToken(request.getEmail(), request.getResetToken());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if(!authenticated){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .authenticated(true)
                .accountType(user.getRole().getName())
                .userId(user.getId().toString())
                .build();
    }

    private String generateToken(User user){
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .issuer("recruitment_system")
                .issueTime(new Date())
                .subject(user.getEmail())
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("user_id", user.getId().toString())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try{
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token: ", e);
            throw new RuntimeException(e);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();

        boolean isValid = true;

        try {
            verifyToken(token, false);
        }
        catch (AppException e){
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }


    private String buildScope(User user){
        StringJoiner stringJoiner = new StringJoiner(" ");
        Role role = user.getRole();
        if(role != null){
            stringJoiner.add("ROLE_" + role.getName());
            if(!CollectionUtils.isEmpty(role.getPermissions())){
                role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            }
        }
        return stringJoiner.toString();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signedToken = verifyToken(request.getToken(), true);

            String id = signedToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(id)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e){
            log.info("Token already expired");
        }

    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                    .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(jwsVerifier);

        if(!(verified && expiryTime.after(new Date()))){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }

    public AuthResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        var email = signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findByEmail(email).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND)
        );

        var token = generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .authenticated(true)
                .accountType(user.getRole().getName())
                .userId(user.getId().toString())
                .build();
    }
}
