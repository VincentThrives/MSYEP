package com.vincent.msyep.modules.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory one-time-password store for student login (5-minute validity, 5 attempts). */
@Service
public class OtpService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private record Entry(String code, Instant expiresAt, int attempts) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom();

    /** Generate and store a fresh 6-digit OTP for the identifier. */
    public String generate(String identifier) {
        String code = String.format("%06d", rnd.nextInt(1_000_000));
        store.put(key(identifier), new Entry(code, Instant.now().plus(TTL), 0));
        return code;
    }

    /** Verify a submitted code. Consumes it on success. */
    public boolean verify(String identifier, String code) {
        String k = key(identifier);
        Entry e = store.get(k);
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt()) || e.attempts() >= MAX_ATTEMPTS) {
            store.remove(k);
            return false;
        }
        if (e.code().equals(code == null ? "" : code.trim())) {
            store.remove(k);
            return true;
        }
        store.put(k, new Entry(e.code(), e.expiresAt(), e.attempts() + 1));
        return false;
    }

    private static String key(String identifier) {
        return identifier == null ? "" : identifier.toLowerCase().trim();
    }
}
