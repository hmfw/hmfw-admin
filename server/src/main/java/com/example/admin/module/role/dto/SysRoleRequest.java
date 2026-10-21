package com.example.admin.module.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysRoleRequest {

    /** 新增时为空；修改时必填 */
    private Long id;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "最长 50")
    private String roleName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "[a-zA-Z0-9_:-]{2,50}",
            message = "只能包含字母、数字、下划线、冒号、连字符，长度 2-50")
    private String roleKey;

    private Integer sort;

    /** 数据范围：1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人；null 表示不改动 */
    private Integer dataScope;

    private Integer status;

    @Size(max = 255, message = "最长 255")
    private String remark;

    /** 分配的菜单 id；null 表示不改动 */
    private List<Long> menuIds;

    /** 自定义数据范围（dataScope=2）的部门 id；null 表示不改动 */
    private List<Long> deptIds;
}
