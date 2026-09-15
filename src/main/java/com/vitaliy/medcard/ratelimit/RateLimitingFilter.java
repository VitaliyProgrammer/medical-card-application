package com.vitaliy.medcard.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS =
            Set.of("/api/auth/login", "/api/auth/registration");

    private final LoginRateLimiter loginRateLimiter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (LIMITED_PATHS.contains(request.getRequestURI())) {
            String key = request.getRemoteAddr() + ":" + request.getRequestURI();

            if (!loginRateLimiter.tryAcquire(key)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"Too many attempts. Please try again in a minute!\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
