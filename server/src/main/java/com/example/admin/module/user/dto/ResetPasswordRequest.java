package com.example.admin.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotNull(message = "不能为空")
    private Long id;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 50, message = "长度需在 6-50 之间")
    private String password;
}
