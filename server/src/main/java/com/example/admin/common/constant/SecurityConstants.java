package com.example.admin.common.constant;

public final class SecurityConstants {

    /** 超级管理员通配权限，拥有它即放行一切 */
    public static final String ALL_PERMISSION = "*:*:*";

    /** 超级管理员的角色标识 */
    public static final String ADMIN_ROLE_KEY = "admin";

    /** 过滤器解析 token 失败时，把异常挂到 request 上，交给 EntryPoint 统一输出 */
    public static final String AUTH_ERROR_ATTR = "ADMIN_AUTH_ERROR";

    private SecurityConstants() {
    }
}
