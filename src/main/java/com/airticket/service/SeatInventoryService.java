package com.airticket.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class SeatInventoryService {

    private static final String STOCK_KEY_PREFIX = "flight:stock:";
    private static final long STOCK_KEY_TTL_DAYS = 7L;
    private static final long LUA_SUCCESS = 1L;
    private static final long LUA_STOCK_NOT_ENOUGH = 0L;
    private static final long LUA_STOCK_NOT_INITIALIZED = -1L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> stockDeductScript;

    public SeatInventoryService(
        RedisTemplate<String, Object> redisTemplate,
        DefaultRedisScript<Long> stockDeductScript
    ) {
        this.redisTemplate = redisTemplate;
        this.stockDeductScript = stockDeductScript;
    }

    public boolean deductStock(Long flightId, int seats, int dbAvailableSeats) {
        String stockKey = buildStockKey(flightId);
        ensureStockInitialized(stockKey, dbAvailableSeats);

        Long result = redisTemplate.execute(
            stockDeductScript,
            Collections.singletonList(stockKey),
            String.valueOf(seats)
        );

        if (result == null) {
            throw new RuntimeException("Redis stock deduction returned null");
        }

        if (result == LUA_STOCK_NOT_INITIALIZED) {
            ensureStockInitialized(stockKey, dbAvailableSeats);
            result = redisTemplate.execute(
                stockDeductScript,
                Collections.singletonList(stockKey),
                String.valueOf(seats)
            );
            if (result == null) {
                throw new RuntimeException("Redis stock deduction retry returned null");
            }
        }

        if (result == LUA_SUCCESS) {
            return true;
        }

        if (result == LUA_STOCK_NOT_ENOUGH) {
            return false;
        }

        throw new RuntimeException("Unexpected Redis stock deduction result: " + result);
    }

    public void rollbackStock(Long flightId, int seats) {
        String stockKey = buildStockKey(flightId);
        redisTemplate.opsForValue().increment(stockKey, seats);
        redisTemplate.expire(stockKey, STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
    }

    public void syncStock(Long flightId, int availableSeats) {
        redisTemplate.opsForValue().set(buildStockKey(flightId), availableSeats, STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
    }

    public void restoreStock(Long flightId, int seats, int latestAvailableSeats) {
        String stockKey = buildStockKey(flightId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            redisTemplate.opsForValue().increment(stockKey, seats);
            redisTemplate.expire(stockKey, STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
            return;
        }

        syncStock(flightId, latestAvailableSeats);
    }

    private void ensureStockInitialized(String stockKey, int dbAvailableSeats) {
        Boolean initialized = redisTemplate.opsForValue().setIfAbsent(
            stockKey,
            dbAvailableSeats,
            STOCK_KEY_TTL_DAYS,
            TimeUnit.DAYS
        );

        if (Boolean.FALSE.equals(initialized)) {
            redisTemplate.expire(stockKey, STOCK_KEY_TTL_DAYS, TimeUnit.DAYS);
        }
    }

    private String buildStockKey(Long flightId) {
        return STOCK_KEY_PREFIX + flightId;
    }
}
