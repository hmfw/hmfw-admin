package com.example.admin.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 不含 username 与 password —— 账号名不可改，改密码走独立的重置接口。
 */
@Data
public class SysUserUpdateRequest {

    @NotNull(message = "不能为空")
    private Long id;

    @Size(max = 50, message = "最长 50")
    private String nickname;

    @Email(message = "格式不正确")
    @Size(max = 100, message = "最长 100")
    private String email;

    @Size(max = 20, message = "最长 20")
    private String phone;

    /** 所属部门 id */
    private Long deptId;

    private Integer status;

    @Size(max = 255, message = "最长 255")
    private String remark;

    private List<Long> roleIds;
}
