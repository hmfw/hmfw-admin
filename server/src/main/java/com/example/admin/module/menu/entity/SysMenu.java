package com.example.admin.module.menu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单 id，0 为根 */
    private Long parentId;

    private String menuName;

    /** M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 前端路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识，如 system:user:list */
    private String perms;

    private String icon;

    private Integer sort;

    /** 1 显示 0 隐藏 */
    private Integer visible;

    /** 1 启用 0 停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
