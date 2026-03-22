package com.airticket.service;

import com.airticket.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final JwtUtil jwtUtil;
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public TokenBlacklistService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        cleanupExpiredTokens();

        Instant expiresAt;
        try {
            expiresAt = jwtUtil.extractExpiration(token).toInstant();
        } catch (Exception ex) {
            expiresAt = Instant.now();
        }

        blacklistedTokens.put(token, expiresAt);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Instant expiresAt = blacklistedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }

        if (expiresAt.isAfter(Instant.now())) {
            return true;
        }

        blacklistedTokens.remove(token);
        return false;
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
