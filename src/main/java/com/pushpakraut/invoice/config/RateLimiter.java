package com.pushpakraut.invoice.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window counter per key (an IP address plus an action).
 * shortcut: in-memory, one server only; use Redis or the host's rate limiting if you run several instances.
 */
public class RateLimiter {
    private record Window(long start, int count) {}

    private final int limit;
    private final long windowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    /** Returns true if this call is allowed, and counts it. */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        if (windows.size() > 50_000) windows.values().removeIf(w -> now - w.start() >= windowMillis); // keep memory bounded
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start() >= windowMillis ? new Window(now, 1) : new Window(old.start(), old.count() + 1));
        return w.count() <= limit;
    }
}
