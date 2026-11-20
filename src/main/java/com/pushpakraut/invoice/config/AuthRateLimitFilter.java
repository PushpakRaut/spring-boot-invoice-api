package com.pushpakraut.invoice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Limits login and register attempts per IP, so passwords cannot be guessed at speed. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter limiter;

    public AuthRateLimitFilter(@Value("${app.auth-attempts-per-ip:10}") int attempts,
                               @Value("${app.auth-window-minutes:10}") long minutes) {
        this.limiter = new RateLimiter(attempts, minutes * 60_000);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !("POST".equals(req.getMethod()) && req.getRequestURI().startsWith("/api/auth/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // Behind the host's proxy, server.forward-headers-strategy makes this the real client IP
        if (!limiter.tryAcquire(req.getRemoteAddr())) {
            res.setHeader("Retry-After", "600");
            res.sendError(429, "Too many attempts. Please wait a few minutes.");
            return;
        }
        chain.doFilter(req, res);
    }
}
