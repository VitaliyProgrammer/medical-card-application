package com.vitaliy.medcard.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class LoginRateLimiterTest {

    @Test
    @DisplayName("tryAcquire: lets requests through when Redis is unavailable (fail-open)")
    @SuppressWarnings("unchecked")
    void redisDown_failsOpen() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        LoginRateLimiter limiter = new LoginRateLimiter(redisTemplate, 5, Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("127.0.0.1:/api/auth/login")).isTrue();
    }
}
