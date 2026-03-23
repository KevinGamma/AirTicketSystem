package com.airticket.service;

import com.airticket.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenBlacklistService(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            Duration remainingValidity = jwtUtil.getRemainingValidity(token);
            if (remainingValidity.isZero() || remainingValidity.isNegative()) {
                return;
            }

            redisTemplate.opsForValue().set(buildBlacklistKey(token), "LOGGED_OUT", remainingValidity);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new RuntimeException("Invalid JWT token", ex);
        }
    }

    public void blacklistToken(String token) {
        logout(token);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return Optional.ofNullable(redisTemplate.hasKey(buildBlacklistKey(token))).orElse(false);
    }

    private String buildBlacklistKey(String token) {
        return BLACKLIST_KEY_PREFIX + token;
    }
}
