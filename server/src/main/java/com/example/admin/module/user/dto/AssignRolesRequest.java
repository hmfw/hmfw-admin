package com.example.admin.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配角色请求：roleIds 传空数组表示清空该用户的角色。
 */
@Data
public class AssignRolesRequest {

    @NotNull(message = "不能为空")
    private Long id;

    private List<Long> roleIds;
}
