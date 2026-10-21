package com.example.admin.module.auth.dto;

import lombok.Data;

/**
 * 个人中心展示用：当前登录用户的基础资料。
 *
 * <p>比 {@link UserInfoVO}（角色/权限集合）更贴近"个人资料表单"，含邮箱/手机号。
 */
@Data
public class ProfileVO {

    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
}
