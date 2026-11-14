package com.vitaliy.medcard.ratelimit;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "rate-limit:";

    private static final RedisScript<Long> INCREMENT_WITH_WINDOW = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.rate-limit.window:1m}") Duration window) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    public boolean tryAcquire(String key) {
        try {
            Long attempts = redisTemplate.execute(INCREMENT_WITH_WINDOW,
                    List.of(KEY_PREFIX + key), String.valueOf(window.toMillis()));
            return attempts != null && attempts <= maxAttempts;
        } catch (DataAccessException exception) {
            log.warn("Rate limiter store is unavailable, letting the request through",
                    exception);
            return true;
        }
    }
}
