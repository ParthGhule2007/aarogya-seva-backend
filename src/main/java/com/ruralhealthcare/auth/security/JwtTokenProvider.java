package com.ruralhealthcare.auth.security;

import com.ruralhealthcare.auth.domain.User;
import com.ruralhealthcare.auth.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .claim("phoneNumber", user.getPhoneNumber())
                .claim("associatedEntityId", user.getAssociatedEntityId() != null ? user.getAssociatedEntityId().toString() : null)
                .issuer("aarogya-seva")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Optional<UserPrincipal> parseAndValidateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            String roleStr = claims.get("role", String.class);
            String fullName = claims.get("fullName", String.class);
            String phoneNumber = claims.get("phoneNumber", String.class);
            String entityIdStr = claims.get("associatedEntityId", String.class);

            String associatedEntityId = (entityIdStr != null && !entityIdStr.isBlank()) 
                    ? entityIdStr.trim() 
                    : null;

            UserRole role = UserRole.fromString(roleStr);

            return Optional.of(new UserPrincipal(userId, username, role, fullName, phoneNumber, associatedEntityId));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long getExpirationMs() {
        return jwtProperties.getExpirationMs();
    }
}
