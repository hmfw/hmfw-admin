package com.example.admin.common.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter();
        // 窗口内放行 3 次，第 4 次拒绝
        assertThat(limiter.tryAcquire("k", 3, 60)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, 60)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, 60)).isTrue();
        assertThat(limiter.tryAcquire("k", 3, 60)).isFalse();
    }

    @Test
    void differentKeysCountedSeparately() {
        RateLimiter limiter = new RateLimiter();
        assertThat(limiter.tryAcquire("a", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("a", 1, 60)).isFalse();
        // 另一个 key 不受影响
        assertThat(limiter.tryAcquire("b", 1, 60)).isTrue();
    }

    @Test
    void windowResetAllowsAgain() throws Exception {
        RateLimiter limiter = new RateLimiter();
        // 1 秒窗口用满
        assertThat(limiter.tryAcquire("w", 1, 1)).isTrue();
        assertThat(limiter.tryAcquire("w", 1, 1)).isFalse();
        Thread.sleep(1100);
        // 窗口过期后重新放行
        assertThat(limiter.tryAcquire("w", 1, 1)).isTrue();
    }
}
