package com.example.admin.module.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配菜单权限请求：menuIds 传空数组表示清空该角色的权限。
 */
@Data
public class AssignMenusRequest {

    @NotNull(message = "不能为空")
    private Long id;

    private List<Long> menuIds;
}
