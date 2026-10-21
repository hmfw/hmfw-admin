package com.example.admin.module.auth.dto;

import lombok.Data;

/**
 * 验证码响应（GET /api/auth/captcha）。
 *
 * <p>{@code enabled=false} 时前端应隐藏验证码输入框，直接登录。
 */
@Data
public class CaptchaVO {

    /** 验证码功能是否开启 */
    private boolean enabled;

    /** 验证码标识，登录时回传 */
    private String captchaKey;

    /** base64 PNG 图片（data:image/png;base64,...） */
    private String captchaImage;
}
