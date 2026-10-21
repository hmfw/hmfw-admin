package com.example.admin.common.api;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),

    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统异常，请联系管理员"),

    LOGIN_FAILED(1001, "用户名或密码错误"),
    ACCOUNT_DISABLED(1002, "账号已停用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    ROLE_KEY_EXISTS(1004, "角色标识已存在"),
    DATA_NOT_FOUND(1005, "数据不存在"),
    MENU_HAS_CHILDREN(1006, "存在子节点，无法删除"),
    ROLE_IN_USE(1007, "角色已分配给用户，无法删除"),
    MENU_IN_USE(1008, "菜单已被角色引用，无法删除"),
    OLD_PASSWORD_WRONG(1009, "原密码不正确"),
    CAPTCHA_ERROR(1010, "验证码错误或已过期"),
    TOO_MANY_REQUESTS(1011, "操作过于频繁，请稍后再试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
