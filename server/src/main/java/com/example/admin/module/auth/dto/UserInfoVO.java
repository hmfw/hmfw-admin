package com.example.admin.module.auth.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UserInfoVO {

    private Long userId;
    private String username;
    private String nickname;

    /** 角色标识，如 admin */
    private Set<String> roles;

    /** 权限标识；超级管理员含 "*:*:*" */
    private Set<String> permissions;
}
