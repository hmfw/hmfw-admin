package com.example.admin.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "不能为空")
    private String username;

    @NotBlank(message = "不能为空")
    private String password;

    /** 验证码标识（来自 /api/auth/captcha）；验证码关闭时可不传 */
    private String captchaKey;

    /** 验证码；验证码关闭时可不传 */
    private String captchaCode;
}
