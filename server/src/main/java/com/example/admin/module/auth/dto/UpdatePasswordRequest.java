package com.example.admin.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户修改自己的密码：需校验原密码。
 */
@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度需在 6-50 之间")
    private String newPassword;
}
