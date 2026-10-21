package com.example.admin.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "不能为空")
    private Long id;

    /** 1 启用 0 停用 */
    @NotNull(message = "不能为空")
    @Min(value = 0, message = "只能是 0 或 1")
    @Max(value = 1, message = "只能是 0 或 1")
    private Integer status;
}
