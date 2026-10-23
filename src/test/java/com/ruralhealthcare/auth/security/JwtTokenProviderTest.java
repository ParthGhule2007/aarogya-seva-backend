package com.ruralhealthcare.auth.security;

import com.ruralhealthcare.auth.domain.User;
import com.ruralhealthcare.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-at-least-32-bytes-long-for-hmac-sha-256");
        jwtProperties.setExpirationMs(3600000L); // 1 hour
        jwtProperties.setEnabled(true);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse principal correctly")
    void testGenerateAndParseToken() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("patient.test")
                .fullName("Test Patient")
                .phoneNumber("9876543210")
                .role(UserRole.PATIENT)
                .associatedEntityId(patientId.toString())
                .build();

        String token = jwtTokenProvider.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Optional<UserPrincipal> principalOpt = jwtTokenProvider.parseAndValidateToken(token);
        assertTrue(principalOpt.isPresent());

        UserPrincipal principal = principalOpt.get();
        assertEquals(userId, principal.getUserId());
        assertEquals("patient.test", principal.getUsername());
        assertEquals(UserRole.PATIENT, principal.getRole());
        assertEquals("Test Patient", principal.getFullName());
        assertEquals("9876543210", principal.getPhoneNumber());
        assertEquals(patientId.toString(), principal.getAssociatedEntityId());
        assertTrue(principal.isPatient());
        assertFalse(principal.isDoctor());
    }

    @Test
    @DisplayName("Should reject malformed or tampered token")
    void testRejectInvalidToken() {
        Optional<UserPrincipal> result = jwtTokenProvider.parseAndValidateToken("invalid.jwt.token");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        jwtProperties.setExpirationMs(-1000L); // already expired
        JwtTokenProvider expiredProvider = new JwtTokenProvider(jwtProperties);

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("expired.user")
                .fullName("Expired User")
                .role(UserRole.PATIENT)
                .build();

        String expiredToken = expiredProvider.generateToken(user);
        Optional<UserPrincipal> result = jwtTokenProvider.parseAndValidateToken(expiredToken);
        assertTrue(result.isEmpty());
    }
}
