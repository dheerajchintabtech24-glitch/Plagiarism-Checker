package com.plagchecker.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RateLimitFilter — Simple token-bucket style rate limiting.
 *
 * Limits each IP to a maximum number of requests per minute.
 * Returns HTTP 429 (Too Many Requests) when the limit is exceeded.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_MS = 60_000; // 1 minute

    // IP -> bucket
    private final Map<String, RateEntry> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // Only rate-limit API endpoints, not static files
        if (!path.startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(req);
        RateEntry entry = buckets.computeIfAbsent(clientIp, k -> new RateEntry());

        long now = System.currentTimeMillis();

        // Reset window if expired
        if (now - entry.windowStart.get() > WINDOW_MS) {
            entry.windowStart.set(now);
            entry.count.set(0);
        }

        if (entry.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " requests/minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateEntry {
        AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        AtomicInteger count = new AtomicInteger(0);
    }
}
