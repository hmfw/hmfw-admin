package com.example.admin.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流：在时间窗口内，同一客户端(IP)对该方法的调用次数上限。
 *
 * <p>进程内计数（单实例足够），多实例/生产应换 Redis。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内允许的最大次数 */
    int count() default 10;

    /** 时间窗口（秒） */
    int seconds() default 60;
}
