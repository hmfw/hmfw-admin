package com.example.admin.common.exception;

import com.example.admin.common.api.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * <p>默认 HTTP 200 + 业务错误码（国内后台的常见约定，前端只需判断 code）。
 * 但认证/授权类错误必须走真实 HTTP 状态，否则「密码错误返回 200」和
 * 「未登录返回 401」两套行为并存，前端拦截器无法统一处理。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BizException(ResultCode resultCode) {
        this(resultCode, resultCode.getMsg(), HttpStatus.OK);
    }

    public BizException(ResultCode resultCode, String msg) {
        this(resultCode, msg, HttpStatus.OK);
    }

    public BizException(ResultCode resultCode, String msg, HttpStatus httpStatus) {
        super(msg);
        this.code = resultCode.getCode();
        this.httpStatus = httpStatus.value();
    }

    /** 认证失败：HTTP 401 + 业务码 */
    public static BizException unauthorized(ResultCode resultCode) {
        return new BizException(resultCode, resultCode.getMsg(), HttpStatus.UNAUTHORIZED);
    }

    /** 无权限：HTTP 403 + 业务码 */
    public static BizException forbidden(ResultCode resultCode) {
        return new BizException(resultCode, resultCode.getMsg(), HttpStatus.FORBIDDEN);
    }

    public static BizException forbidden(ResultCode resultCode, String msg) {
        return new BizException(resultCode, msg, HttpStatus.FORBIDDEN);
    }

    /** 触发限流：HTTP 429 + 业务码 */
    public static BizException tooManyRequests(ResultCode resultCode) {
        return new BizException(resultCode, resultCode.getMsg(), HttpStatus.TOO_MANY_REQUESTS);
    }
}
