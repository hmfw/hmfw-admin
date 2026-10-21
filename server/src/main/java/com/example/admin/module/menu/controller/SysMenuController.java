package com.example.admin.module.menu.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.menu.dto.SysMenuRequest;
import com.example.admin.module.menu.dto.SysMenuTreeVO;
import com.example.admin.module.menu.dto.SysMenuVO;
import com.example.admin.module.menu.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @Operation(summary = "菜单树", description = "可按名称/状态过滤，返回仍是树形结构")
    @PreAuthorize("@ps.has('system:menu:list')")
    @GetMapping("/tree")
    public R<List<SysMenuTreeVO>> tree(@RequestParam(required = false) String menuName,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(sysMenuService.tree(menuName, status));
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("@ps.has('system:menu:query')")
    @GetMapping("/info")
    public R<SysMenuVO> detail(@RequestParam Long id) {
        return R.ok(sysMenuService.getDetail(id));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("@ps.has('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysMenuRequest request) {
        return R.ok(sysMenuService.create(request));
    }

    @Operation(summary = "修改菜单", description = "父菜单不能是自己或自己的子节点")
    @PreAuthorize("@ps.has('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysMenuRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysMenuService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除菜单", description = "有子节点或已被角色引用时不允许删除")
    @PreAuthorize("@ps.has('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        request.getIds().forEach(sysMenuService::delete);
        return R.ok();
    }
}
