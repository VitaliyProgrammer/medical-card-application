package com.vitaliy.medcard.security;

import com.vitaliy.medcard.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private static final String ROLE_CLAIM = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private final Key secret;
    private final JwtParser parser;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secretString,
                    @Value("${jwt.expiration}") long expiration) {
        byte[] secretBytes = secretString.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be at least "
                    + MIN_SECRET_BYTES + " bytes long for HS256, got " + secretBytes.length);
        }
        this.secret = Keys.hmacShaKeyFor(secretBytes);
        this.parser = Jwts.parserBuilder().setSigningKey(secret).build();
        this.expiration = expiration;
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim(ROLE_CLAIM, role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(secret)
                .compact();
    }

    public JwtPrincipal parseToken(String token) {
        try {
            Claims claims = parser.parseClaimsJws(token).getBody();
            return new JwtPrincipal(claims.getSubject(), claims.get(ROLE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtAuthenticationException("Expired or invalid JWT token!");
        }
    }

    public record JwtPrincipal(String email, String role) {
    }
}
