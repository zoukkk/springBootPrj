package com.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret:}")
    private String configuredSecret;

    @Value("${app.jwt.expires-in-seconds:7200}")
    private long expiresInSeconds;

    private Algorithm algorithm;

    @PostConstruct
    void initialize() {
        String secret = configuredSecret;
        if (!StringUtils.hasText(secret)) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            secret = Base64.getEncoder().encodeToString(bytes);
            log.warn("JWT_SECRET 未配置，已生成仅在本次进程有效的随机密钥");
        }
        algorithm = Algorithm.HMAC256(secret);
    }

    public String createToken(AuthenticatedUser user, String tokenId, Instant issuedAt) {
        Instant expiresAt = issuedAt.plusSeconds(expiresInSeconds);
        return JWT.create()
                .withJWTId(tokenId)
                .withSubject(String.valueOf(user.userId()))
                .withClaim("userId", user.userId())
                .withClaim("username", user.username())
                .withClaim("roles", user.roles())
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public TokenClaims parse(String token) {
        DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
        return new TokenClaims(
                jwt.getId(),
                jwt.getClaim("userId").asLong(),
                jwt.getClaim("username").asString(),
                jwt.getClaim("roles").asList(String.class),
                jwt.getIssuedAtAsInstant(),
                jwt.getExpiresAtAsInstant()
        );
    }

    public String newTokenId() {
        return UUID.randomUUID().toString();
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public record TokenClaims(String tokenId, Long userId, String username, List<String> roles,
                              Instant issuedAt, Instant expiresAt) {
    }
}
