package com.example.admin.security;

import com.example.admin.common.constant.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 供 {@code @PreAuthorize("@ps.has('system:user:list')")} 调用的鉴权 bean。
 *
 * <p>超级管理员在 UserDetailsServiceImpl 装载时就被塞入 "*:*:*"，
 * 这里只需判断通配符即可放行一切 —— 单一真相源，前端也能从 /auth/me 直接读到。
 */
@Component("ps")
public class PermissionService {

    public boolean has(String permission) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        Set<String> perms = user.getPermissions();
        return perms.contains(SecurityConstants.ALL_PERMISSION) || perms.contains(permission);
    }

    public boolean hasAny(String... permissions) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        Set<String> perms = user.getPermissions();
        if (perms.contains(SecurityConstants.ALL_PERMISSION)) {
            return true;
        }
        for (String permission : permissions) {
            if (perms.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String roleKey) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        return user != null && user.getRoles().contains(roleKey);
    }
}
