package com.example.admin.module.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysMenuRequest {

    /** 新增时为空；修改时必填 */
    private Long id;

    @NotNull(message = "不能为空")
    private Long parentId;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "最长 50")
    private String menuName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "[MCF]", message = "只能是 M(目录) / C(菜单) / F(按钮)")
    private String menuType;

    @Size(max = 200, message = "最长 200")
    private String path;

    @Size(max = 255, message = "最长 255")
    private String component;

    @Size(max = 100, message = "最长 100")
    private String perms;

    @Size(max = 100, message = "最长 100")
    private String icon;

    private Integer sort;

    private Integer visible;

    private Integer status;
}
