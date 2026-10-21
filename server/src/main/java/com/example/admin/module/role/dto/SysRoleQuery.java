package com.example.admin.module.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SysRoleQuery {

    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "至少 1 条")
    @Max(value = 500, message = "最多 500 条")
    private long pageSize = 10;

    /** 模糊匹配角色名 */
    private String roleName;

    private Integer status;
}
