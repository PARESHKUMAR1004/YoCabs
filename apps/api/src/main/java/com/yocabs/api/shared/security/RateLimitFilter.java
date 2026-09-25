package com.yocabs.api.shared.security;

import com.yocabs.api.shared.interfaces.rest.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, per-client-IP limiter for unauthenticated abuse-prone
 * endpoints. In-memory: use a shared store (Redis) when running several
 * API instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int otpPerMinute;
    private final int searchPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${yocabs.rate-limit.otp-per-minute:5}") int otpPerMinute,
            @Value("${yocabs.rate-limit.search-per-minute:60}") int searchPerMinute
    ) {
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.otpPerMinute = otpPerMinute;
        this.searchPerMinute = searchPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        int limit = limitFor(request);

        if (limit > 0 && !tryAcquire(request.getRequestURI() + "|" + request.getRemoteAddr(), limit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(
                            new ErrorResponse(
                                    "TOO_MANY_REQUESTS",
                                    "Too many requests. Please try again later.",
                                    CorrelationIdFilter.current()
                            )
                    )
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {

        if (!"POST".equals(request.getMethod())) {
            return 0;
        }

        String path = request.getRequestURI();

        if (path.equals("/api/v1/auth/otp/request")
                || path.equals("/api/v1/auth/otp/verify")
                || path.equals("/api/v1/auth/admin/login")) {
            return otpPerMinute;
        }

        if (path.equals("/api/v1/trip-search")) {
            return searchPerMinute;
        }

        return 0;
    }

    private boolean tryAcquire(String key, int limit) {

        long now = clock.millis();

        if (windows.size() > 10_000) {
            windows.values().removeIf(window -> now - window.start > WINDOW_MILLIS);
        }

        Window window =
                windows.compute(key, (k, existing) ->
                        existing == null || now - existing.start > WINDOW_MILLIS
                                ? new Window(now)
                                : existing);

        synchronized (window) {
            window.count++;
            return window.count <= limit;
        }
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}
