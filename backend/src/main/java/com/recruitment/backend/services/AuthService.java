package com.recruitment.backend.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.recruitment.backend.domain.dtos.*;
import com.recruitment.backend.domain.entities.User;
import com.recruitment.backend.exceptions.AppException;
import com.recruitment.backend.exceptions.ErrorCode;
import com.recruitment.backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    @Value("${jwt.signerKey}")
    private String signerKey;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
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
                .claim("scope", user.getRole().toString())
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
        JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(jwsVerifier);
        return IntrospectResponse.builder()
                .valid(verified && expiryTime.after(new Date()))
                .build();
    }


//    private String buildScope(User user){
//        StringJoiner stringJoiner = new StringJoiner(" ");
//        if(!CollectionUtils.isEmpty(user.getRole())){
//            user.getRole().
//        }
//    }


}
