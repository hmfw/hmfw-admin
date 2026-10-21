package com.example.admin.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysUserCreateRequest {

    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 50, message = "长度需在 3-50 之间")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 50, message = "长度需在 6-50 之间")
    private String password;

    @Size(max = 50, message = "最长 50")
    private String nickname;

    @Email(message = "格式不正确")
    @Size(max = 100, message = "最长 100")
    private String email;

    @Size(max = 20, message = "最长 20")
    private String phone;

    /** 所属部门 id */
    private Long deptId;

    /** 不传按启用处理 */
    private Integer status;

    @Size(max = 255, message = "最长 255")
    private String remark;

    private List<Long> roleIds;
}
