package com.example.admin.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SysUserQuery {

    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "至少 1 条")
    @Max(value = 500, message = "最多 500 条")
    private long pageSize = 10;

    /** 模糊匹配 */
    private String username;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 按部门过滤（点击部门树时传） */
    private Long deptId;
}
