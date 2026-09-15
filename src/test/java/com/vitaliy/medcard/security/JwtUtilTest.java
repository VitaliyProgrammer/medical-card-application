package com.vitaliy.medcard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitaliy.medcard.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-secret-key-for-jwt-util-unit-tests-1234567890", 604_800_000L);
    }

    @Test
    @DisplayName("generateToken: encodes the email and role, both readable back from the token")
    void generateAndReadToken() {
        String token = jwtUtil.generateToken("doctor@test.com", "DOCTOR");

        assertThat(jwtUtil.isValidToken(token)).isTrue();
        assertThat(jwtUtil.getEmailFromToken(token)).isEqualTo("doctor@test.com");
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("DOCTOR");
    }

    @Test
    @DisplayName("isValidToken: rejects a garbage token")
    void isValidToken_garbageToken() {
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtil.isValidToken("not-a-real-token"));
    }

    @Test
    @DisplayName("isValidToken: rejects a token signed with a different secret")
    void isValidToken_wrongSignature() {
        JwtUtil otherJwtUtil = new JwtUtil(
                "a-completely-different-secret-key-1234567890", 604_800_000L);
        String token = otherJwtUtil.generateToken("doctor@test.com", "DOCTOR");

        assertThrows(JwtAuthenticationException.class, () -> jwtUtil.isValidToken(token));
    }

    @Test
    @DisplayName("isValidToken: rejects an already expired token")
    void isValidToken_expiredToken() {
        JwtUtil shortLivedJwtUtil = new JwtUtil(
                "test-secret-key-for-jwt-util-unit-tests-1234567890", -1_000L);
        String token = shortLivedJwtUtil.generateToken("doctor@test.com", "DOCTOR");

        assertThrows(JwtAuthenticationException.class, () -> jwtUtil.isValidToken(token));
    }
}
