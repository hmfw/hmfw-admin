package com.example.admin.common.log;

import com.example.admin.module.monitor.operlog.entity.SysOperLog;
import com.example.admin.module.monitor.operlog.service.SysOperLogService;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * 操作日志切面：拦截标了 {@link Log} 的方法，记录到 sys_oper_log。
 *
 * <p>日志落库失败只打警告，绝不影响主流程（也不吞掉业务异常——异常照常抛出）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MAX_LEN = 2000;

    private final SysOperLogService sysOperLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, Log logAnno) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable thrown = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            thrown = e;
            throw e;
        } finally {
            try {
                saveLog(point, logAnno, result, thrown, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, Log logAnno, Object result, Throwable thrown, long cost) {
        SysOperLog entity = new SysOperLog();
        entity.setTitle(logAnno.title());
        entity.setBusinessType(logAnno.businessType().name());

        MethodSignature signature = (MethodSignature) point.getSignature();
        entity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName() + "()");

        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        entity.setOperName(loginUser == null ? null : loginUser.getUsername());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setRequestMethod(request.getMethod());
            entity.setOperUrl(truncate(request.getRequestURI()));
            entity.setOperIp(clientIp(request));
        }

        if (logAnno.saveRequestData()) {
            entity.setOperParam(truncate(toJson(filterArgs(point.getArgs()))));
        }

        if (thrown == null) {
            entity.setStatus(0);
            entity.setJsonResult(truncate(toJson(result)));
        } else {
            entity.setStatus(1);
            entity.setErrorMsg(truncate(thrown.getMessage()));
        }

        entity.setCostTime(cost);
        entity.setOperTime(LocalDateTime.now(ZONE));
        sysOperLogService.save(entity);
    }

    /** 过滤掉无法/不宜序列化的参数（Servlet 对象等） */
    private Object[] filterArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        return Arrays.stream(args)
                .filter(a -> !(a instanceof HttpServletRequest)
                        && !(a instanceof HttpServletResponse))
                .toArray();
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_LEN ? s.substring(0, MAX_LEN) : s;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }
}
