package com.example.admin.module.role.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.role.dto.AssignMenusRequest;
import com.example.admin.module.role.dto.SysRoleQuery;
import com.example.admin.module.role.dto.SysRoleRequest;
import com.example.admin.module.role.dto.SysRoleVO;
import com.example.admin.module.role.service.SysRoleService;
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

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @Operation(summary = "分页查询角色")
    @PreAuthorize("@ps.has('system:role:list')")
    @GetMapping("/list")
    public R<PageResult<SysRoleVO>> page(@Valid SysRoleQuery query) {
        return R.ok(sysRoleService.page(query));
    }

    @Operation(summary = "角色字典", description = "返回启用角色的 label/value 选项，供前端下拉使用")
    @PreAuthorize("@ps.has('system:role:list')")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.ok(sysRoleService.listOptions());
    }

    @Operation(summary = "角色详情", description = "含已分配的菜单 id")
    @PreAuthorize("@ps.has('system:role:query')")
    @GetMapping("/info")
    public R<SysRoleVO> detail(@RequestParam Long id) {
        return R.ok(sysRoleService.getDetail(id));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("@ps.has('system:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysRoleRequest request) {
        return R.ok(sysRoleService.create(request));
    }

    @Operation(summary = "修改角色")
    @PreAuthorize("@ps.has('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysRoleRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysRoleService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除角色", description = "内置管理员角色与已分配用户的角色不可删")
    @PreAuthorize("@ps.has('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        request.getIds().forEach(sysRoleService::delete);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限", description = "传空数组表示清空该角色的权限")
    @PreAuthorize("@ps.has('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PostMapping("/assignMenus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest request) {
        sysRoleService.assignMenus(request.getId(), request.getMenuIds());
        return R.ok();
    }
}
