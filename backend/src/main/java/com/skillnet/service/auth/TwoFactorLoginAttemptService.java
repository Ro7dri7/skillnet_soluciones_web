package com.skillnet.service.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Rate-limit de intentos 2FA en login (en memoria; suficiente para dev/single-node). */
@Service
public class TwoFactorLoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;

    private final Map<Long, Integer> failures = new ConcurrentHashMap<>();
    private final Map<Long, Long> lockedUntil = new ConcurrentHashMap<>();

    public boolean isLocked(Long userId) {
        Long until = lockedUntil.get(userId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            lockedUntil.remove(userId);
            failures.remove(userId);
            return false;
        }
        return true;
    }

    public void recordFailure(Long userId) {
        int count = failures.merge(userId, 1, Integer::sum);
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(userId, System.currentTimeMillis() + LOCK_MS);
            failures.remove(userId);
        }
    }

    public void clearFailures(Long userId) {
        failures.remove(userId);
        lockedUntil.remove(userId);
    }
}
