package com.agripulse.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Locks a key (a login name, a user id, an IP address) for 15 minutes after 5 failures inside 15 minutes.
 * Kept in memory, so it resets when the app restarts and applies to a single server only.
 */
@Component
public class LoginThrottle {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record State(int failures, Instant first, Instant lockedUntil) {}

    private final Map<String, State> states = new HashMap<>();

    public synchronized void check(String key) {
        State s = states.get(key);
        if (s == null || s.lockedUntil() == null) return;
        if (Instant.now().isBefore(s.lockedUntil()))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait 15 minutes and try again.");
        states.remove(key);
    }

    public synchronized void failure(String key) {
        Instant now = Instant.now();
        if (states.size() > 10000)
            states.values().removeIf(v -> v.first().plus(WINDOW).isBefore(now)
                    && (v.lockedUntil() == null || v.lockedUntil().isBefore(now)));
        State s = states.get(key);
        if (s == null || now.isAfter(s.first().plus(WINDOW))) s = new State(0, now, null);
        int n = s.failures() + 1;
        states.put(key, new State(n, s.first(), n >= MAX_FAILURES ? now.plus(WINDOW) : null));
    }

    public synchronized void success(String key) {
        states.remove(key);
    }
}
