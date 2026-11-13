package com.vitaliy.medcard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitaliy.medcard.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-jwt-util-unit-tests-1234567890";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 604_800_000L);
    }

    @Test
    @DisplayName("parseToken: reads back the email and role encoded by generateToken")
    void generateAndParseToken() {
        String token = jwtUtil.generateToken("doctor@test.com", "DOCTOR");

        JwtUtil.JwtPrincipal principal = jwtUtil.parseToken(token);

        assertThat(principal.email()).isEqualTo("doctor@test.com");
        assertThat(principal.role()).isEqualTo("DOCTOR");
    }

    @Test
    @DisplayName("constructor: refuses to start with a secret shorter than 256 bits")
    void constructor_shortSecret_failsFast() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short-secret", 60_000L));

        assertThat(exception.getMessage()).contains("JWT_SECRET").contains("32 bytes");
    }

    @Test
    @DisplayName("parseToken: rejects a garbage token")
    void parseToken_garbageToken() {
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtil.parseToken("not-a-real-token"));
    }

    @Test
    @DisplayName("parseToken: rejects a token signed with a different secret")
    void parseToken_wrongSignature() {
        JwtUtil otherJwtUtil = new JwtUtil(
                "a-completely-different-secret-key-1234567890", 604_800_000L);
        String token = otherJwtUtil.generateToken("doctor@test.com", "DOCTOR");

        assertThrows(JwtAuthenticationException.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    @DisplayName("parseToken: rejects an already expired token")
    void parseToken_expiredToken() {
        JwtUtil shortLivedJwtUtil = new JwtUtil(SECRET, -1_000L);
        String token = shortLivedJwtUtil.generateToken("doctor@test.com", "DOCTOR");

        assertThrows(JwtAuthenticationException.class, () -> jwtUtil.parseToken(token));
    }
}
