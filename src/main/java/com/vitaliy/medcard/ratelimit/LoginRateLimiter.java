package com.vitaliy.medcard.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, AttemptWindow> attemptsByKey = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key) {
        return attemptsByKey.computeIfAbsent(key, k -> new AttemptWindow()).tryAcquire();
    }

    @Scheduled(fixedRate = 300_000)
    public void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(WINDOW);
        attemptsByKey.values().removeIf(window -> window.windowStart.isBefore(cutoff));
    }

    private static final class AttemptWindow {
        private Instant windowStart = Instant.now();
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized boolean tryAcquire() {
            Instant now = Instant.now();
            if (Duration.between(windowStart, now).compareTo(WINDOW) > 0) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= MAX_ATTEMPTS;
        }
    }
}
