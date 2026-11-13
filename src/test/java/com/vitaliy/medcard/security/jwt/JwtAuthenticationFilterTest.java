package com.vitaliy.medcard.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitaliy.medcard.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-for-jwt-filter-unit-tests-1234567890", 60_000L);
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a valid bearer token authenticates the request with the token's role")
    void validToken_authenticates() throws Exception {
        String token = jwtUtil.generateToken("doctor@test.com", "DOCTOR");
        MockHttpServletResponse response = doFilter("Bearer " + token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication.getName()).isEqualTo("doctor@test.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_DOCTOR");
    }

    @Test
    @DisplayName("an invalid token is rejected with 401")
    void invalidToken_unauthorized() throws Exception {
        MockHttpServletResponse response = doFilter("Bearer not-a-real-token");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Bearer   ", "Basic abc", "Bearerabc"})
    @DisplayName("a missing or malformed bearer header is ignored, not a server error")
    void malformedHeader_passesThroughUnauthenticated(String header) throws Exception {
        MockHttpServletResponse response = doFilter(header);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletResponse doFilter(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
