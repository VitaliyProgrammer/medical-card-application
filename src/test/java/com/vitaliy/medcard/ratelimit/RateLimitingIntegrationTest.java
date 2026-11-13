package com.vitaliy.medcard.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitaliy.medcard.IntegrationTestBase;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.max-attempts=3")
class RateLimitingIntegrationTest extends IntegrationTestBase {

    private static final String WRONG_LOGIN =
            "{\"email\": \"nobody@test.com\", \"password\": \"Wrong-pass1!\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("the 4th login attempt within the window from one IP gets 429")
    void exceedingLimit_isRejected() throws Exception {
        String ip = randomIp();

        for (int attempt = 0; attempt < 3; attempt++) {
            login(ip).andExpect(status().isUnauthorized());
        }

        login(ip).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("the limit is counted per IP, so another client is not affected")
    void otherIp_isNotAffected() throws Exception {
        String blockedIp = randomIp();
        for (int attempt = 0; attempt < 4; attempt++) {
            login(blockedIp);
        }

        login(randomIp()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("two application instances share one counter through Redis")
    void counterIsSharedAcrossInstances() {
        String key = "shared-" + UUID.randomUUID();
        LoginRateLimiter instanceA = new LoginRateLimiter(redisTemplate, 3, Duration.ofMinutes(1));
        LoginRateLimiter instanceB = new LoginRateLimiter(redisTemplate, 3, Duration.ofMinutes(1));

        assertThat(instanceA.tryAcquire(key)).isTrue();
        assertThat(instanceB.tryAcquire(key)).isTrue();
        assertThat(instanceA.tryAcquire(key)).isTrue();
        assertThat(instanceB.tryAcquire(key)).isFalse();
    }

    @Test
    @DisplayName("every counter key gets a TTL, so a blocked IP is released after the window")
    void counterKeyExpires() {
        String key = "ttl-" + UUID.randomUUID();
        new LoginRateLimiter(redisTemplate, 3, Duration.ofMinutes(1)).tryAcquire(key);

        Long ttlSeconds = redisTemplate.getExpire("rate-limit:" + key);
        assertThat(ttlSeconds).isBetween(1L, 60L);
    }

    private ResultActions login(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(WRONG_LOGIN));
    }

    private static String randomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "."
                + random.nextInt(1, 255);
    }
}
