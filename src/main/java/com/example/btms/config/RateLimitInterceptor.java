package com.example.btms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limiting Interceptor
 * 
 * Prevents excessive requests from single IP/session
 * - Rate limit: 100 requests per minute per IP
 * - Sliding window algorithm
 * - Automatic cleanup of old entries
 * 
 * @author BTMS Team
 * @version 1.0
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    // Rate limit configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute
    private static final long CLEANUP_INTERVAL_MS = 300_000; // 5 minutes
    
    // Store request counts per IP
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Get client IP
        String clientIp = getClientIp(request);
        
        // Get or create counter for this IP
        RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());
        
        // Check rate limit
        if (counter.increment()) {
            // Within limit - allow request
            
            // Periodic cleanup
            periodicCleanup();
            
            return true;
        } else {
            // Rate limit exceeded
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Max %d requests per minute.\",\"retryAfter\":60}",
                MAX_REQUESTS_PER_MINUTE
            ));
            
            return false;
        }
    }
    
    /**
     * Get real client IP (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * Periodic cleanup of old entries
     */
    private void periodicCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            requestCounts.entrySet().removeIf(entry -> 
                now - entry.getValue().getLastRequestTime() > TIME_WINDOW_MS * 2
            );
            lastCleanupTime = now;
        }
    }
    
    /**
     * Request counter with sliding window
     */
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        
        /**
         * Increment counter and check if within limit
         * @return true if within limit, false if exceeded
         */
        public boolean increment() {
            long now = System.currentTimeMillis();
            long start = windowStart.get();
            
            // Reset window if expired
            if (now - start > TIME_WINDOW_MS) {
                windowStart.set(now);
                count.set(1);
                return true;
            }
            
            // Increment and check limit
            int current = count.incrementAndGet();
            return current <= MAX_REQUESTS_PER_MINUTE;
        }
        
        public long getLastRequestTime() {
            return windowStart.get();
        }
    }
}
