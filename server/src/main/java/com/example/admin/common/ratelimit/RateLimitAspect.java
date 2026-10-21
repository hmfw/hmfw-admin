package com.example.admin.common.ratelimit;

import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面：拦截 {@link RateLimit} 方法，按 IP + 方法 计数，超限抛 429。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiter rateLimiter;

    @Value("${admin.ratelimit.enabled:true}")
    private boolean enabled;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        if (enabled) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String key = clientIp() + ":" + signature.getDeclaringTypeName() + "." + signature.getName();
            if (!rateLimiter.tryAcquire(key, rateLimit.count(), rateLimit.seconds())) {
                throw BizException.tooManyRequests(ResultCode.TOO_MANY_REQUESTS);
            }
        }
        return point.proceed();
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
                int comma = xff.indexOf(',');
                return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}
