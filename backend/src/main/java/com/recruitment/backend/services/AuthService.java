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
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request, AccountType accountType) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        Role role = roleRepository.findById(accountType.name()).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        userRepository.save(user);
        var jwtToken = generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .authenticated(true)
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if(!authenticated){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .authenticated(true)
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
                .expirationTime(new Date(new Date().getTime() + 1000 * 60 * 60))
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
            verifyToken(token);
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
        SignedJWT signedToken = verifyToken(request.getToken());

        String id = signedToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(id)
                .expiryTime(expiryTime)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(jwsVerifier);

        if(!(verified && expiryTime.after(new Date()))){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if(invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }
}
