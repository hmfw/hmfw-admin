package com.example.admin.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;

    /** 仅在查询单个用户时填充，列表接口留空以免 N+1 */
    private List<Long> roleIds;

    /** 列表接口批量填充，用于直接展示用户角色 */
    private List<String> roleNames;
}
