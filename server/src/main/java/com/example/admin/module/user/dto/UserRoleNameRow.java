package com.example.admin.module.user.dto;

import lombok.Data;

/** 用户列表批量回填角色名称的查询结果。 */
@Data
public class UserRoleNameRow {

    private Long userId;
    private String roleName;
}
