package com.diamante.delivery.orderservice.ratelimit;

/**
 * Minimal thread-safe token bucket: holds up to {@code permitsPerSecond} tokens and
 * refills continuously at that rate, so at most that many requests pass per second.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillPerNano;

    private double tokens;
    private long lastRefill;

    public TokenBucket(int permitsPerSecond) {
        this.capacity = permitsPerSecond;
        this.refillPerNano = permitsPerSecond / 1_000_000_000.0;
        this.tokens = permitsPerSecond;
        this.lastRefill = System.nanoTime();
    }

    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
        lastRefill = now;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }
}
