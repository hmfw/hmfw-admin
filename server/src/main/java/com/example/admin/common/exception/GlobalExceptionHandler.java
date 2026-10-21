package com.example.admin.common.exception;

import com.example.admin.common.api.R;
import com.example.admin.common.api.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：HTTP 状态由异常自带（普通业务错误 200，认证类 401/403），业务码放在 body */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(R.fail(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler({ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBadRequest(Exception e) {
        return R.fail(ResultCode.BAD_REQUEST, "请求参数不合法: " + e.getMessage());
    }

    /** 唯一约束冲突（用户名、角色标识重复），兜底 MyBatis-Plus 没拦住的场景 */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突", e);
        return R.fail(ResultCode.BAD_REQUEST, "数据已存在（唯一约束冲突）");
    }

    /**
     * 无权限。@PreAuthorize 在 Controller 内抛出时走这里；
     * 过滤器链（requestMatchers）抛出的走 RestAccessDeniedHandler —— 两条路都要配。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthentication(AuthenticationException e) {
        return R.fail(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return R.fail(ResultCode.INTERNAL_ERROR);
    }
}
