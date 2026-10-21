package com.example.admin.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.admin.common.constant.SecurityConstants;
import com.example.admin.module.menu.mapper.SysMenuMapper;
import com.example.admin.module.role.mapper.SysRoleMapper;
import com.example.admin.module.user.entity.SysUser;
import com.example.admin.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // selectOne 受 @TableLogic 保护，已删除用户查不出来
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setDeptId(user.getDeptId());
        // 停用用户照样装载，由 isEnabled() 拦截，这样能给出"账号已停用"而不是"用户不存在"
        loginUser.setStatus(user.getStatus());

        Set<String> roleKeys = new LinkedHashSet<>(sysRoleMapper.selectRoleKeysByUserId(user.getId()));
        loginUser.setRoles(roleKeys);

        Set<String> permissions = new LinkedHashSet<>(sysMenuMapper.selectPermsByUserId(user.getId()));
        if (roleKeys.contains(SecurityConstants.ADMIN_ROLE_KEY)) {
            permissions.add(SecurityConstants.ALL_PERMISSION);
            loginUser.setSuperAdmin(true);
        }
        loginUser.setPermissions(permissions);

        return loginUser;
    }
}
