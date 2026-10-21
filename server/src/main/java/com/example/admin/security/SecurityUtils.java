package com.example.admin.security;

import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 取不到返回 null，供 SpEL 鉴权表达式使用 */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 取不到直接抛 401 */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw BizException.unauthorized(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }
}
