package com.example.admin.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户修改个人资料。
 *
 * <p>只允许改昵称/邮箱/手机号；账号名、状态、角色不在个人中心维护。
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;
}
