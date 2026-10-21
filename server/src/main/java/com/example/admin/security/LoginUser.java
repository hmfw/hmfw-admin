package com.example.admin.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String nickname;
    private String avatar;

    /** 所属部门 id，用于数据范围过滤 */
    private Long deptId;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 角色标识集合，如 admin / readonly */
    private Set<String> roles = new LinkedHashSet<>();

    /** 权限标识集合，超级管理员含 "*:*:*" */
    private Set<String> permissions = new LinkedHashSet<>();

    private boolean superAdmin;

    /** 当前 JWT 的 jti，用于会话注销与在线列表标记。 */
    private String sessionId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
