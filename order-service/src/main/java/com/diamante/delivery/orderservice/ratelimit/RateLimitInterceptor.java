package com.diamante.delivery.orderservice.ratelimit;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bonus: limits POST /orders to N requests per second (default 20).
 * The excess receives 429 Too Many Requests with {"error": "..."}.
 * Set delivery.rate-limit.orders-per-second=0 to disable.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final TokenBucket bucket;

    public RateLimitInterceptor(@Value("${delivery.rate-limit.orders-per-second:20}") int ordersPerSecond) {
        this.bucket = ordersPerSecond > 0 ? new TokenBucket(ordersPerSecond) : null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (bucket == null || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (bucket.tryAcquire()) {
            return true;
        }
        log.warn("Rate limit exceeded on POST /orders, request rejected with 429");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"Too many requests, please try again in a moment\"}");
        return false;
    }
}
