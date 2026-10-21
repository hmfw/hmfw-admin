package com.example.admin.module.dept.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDeptRequest {

    /** 修改时必填，新增时为空 */
    private Long id;

    /** 父部门 id，0 表示根 */
    private Long parentId = 0L;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过 50")
    private String deptName;

    private String leader;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer sort = 0;

    /** 1 启用 0 停用 */
    private Integer status = 1;
}
