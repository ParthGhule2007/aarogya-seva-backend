package com.ruralhealthcare.sos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${security.jwt.secret:aarogya-seva-secret-key-rural-healthcare-jwt-token-2024-secure-key-32bytes}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<UserPrincipal> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.get("role", String.class);
            String associatedEntityId = claims.get("associatedEntityId", String.class);
            if ("PATIENT".equalsIgnoreCase(role) && associatedEntityId != null && !associatedEntityId.isBlank()) {
                userId = UUID.fromString(associatedEntityId);
            }

            return Optional.of(UserPrincipal.of(userId, role));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT token validation failed in SOS service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
