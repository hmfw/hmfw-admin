package com.example.admin.common.api;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应封装：{@code {code, msg, data, timestamp}}
 */
@Getter
@Setter
public class R<T> {

    private int code;
    private String msg;
    private T data;
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(ResultCode rc) {
        return fail(rc.getCode(), rc.getMsg());
    }

    /** 用错误码但自定义提示语，例如校验失败时附上具体是哪个字段 */
    public static <T> R<T> fail(ResultCode rc, String msg) {
        return fail(rc.getCode(), msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
