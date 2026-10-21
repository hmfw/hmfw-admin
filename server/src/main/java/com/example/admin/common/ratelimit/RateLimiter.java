package com.example.admin.common.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定时间窗口计数限流器（进程内）。
 *
 * <p>每个 key 维护一个窗口：窗口内累加计数，超过阈值则拒绝；窗口过期后重置。
 * 线程安全，适合单实例；多实例请换 Redis + Lua。
 */
@Component
public class RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 尝试放行一次调用。
     *
     * @return true 放行；false 超限
     */
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;
        Window window = windows.compute(key, (k, cur) -> {
            if (cur == null || now - cur.start >= windowMs) {
                return new Window(now);
            }
            return cur;
        });
        return window.count.incrementAndGet() <= limit;
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.start = start;
        }
    }
}
