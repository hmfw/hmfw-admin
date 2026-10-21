package com.example.admin.module.menu.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysMenuVO {

    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private LocalDateTime createTime;
}
