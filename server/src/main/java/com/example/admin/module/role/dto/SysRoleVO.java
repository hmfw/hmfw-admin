package com.example.admin.module.role.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysRoleVO {

    private Long id;
    private String roleName;
    private String roleKey;
    private Integer sort;
    private Integer dataScope;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    /** 仅在查详情时填充 */
    private List<Long> menuIds;

    /** 仅在查详情时填充：自定义数据范围（dataScope=2）勾选的部门 id */
    private List<Long> deptIds;
}
