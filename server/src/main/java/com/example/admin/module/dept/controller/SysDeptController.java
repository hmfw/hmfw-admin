package com.example.admin.module.dept.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.OptionVO;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.dept.dto.SysDeptRequest;
import com.example.admin.module.dept.dto.SysDeptTreeVO;
import com.example.admin.module.dept.entity.SysDept;
import com.example.admin.module.dept.service.SysDeptService;
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

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @Operation(summary = "部门树", description = "可按名称/状态过滤")
    @PreAuthorize("@ps.hasAny('system:dept:list', 'system:user:list')")
    @GetMapping("/tree")
    public R<List<SysDeptTreeVO>> tree(@RequestParam(required = false) String deptName,
                                       @RequestParam(required = false) Integer status) {
        return R.ok(sysDeptService.tree(deptName, status));
    }

    @Operation(summary = "部门字典", description = "启用部门的 id/名称，供下拉选择")
    @PreAuthorize("@ps.has('system:dept:list')")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.ok(sysDeptService.listOptions());
    }

    @Operation(summary = "部门详情")
    @PreAuthorize("@ps.has('system:dept:query')")
    @GetMapping("/info")
    public R<SysDept> detail(@RequestParam Long id) {
        return R.ok(sysDeptService.getDetail(id));
    }

    @Operation(summary = "新增部门")
    @PreAuthorize("@ps.has('system:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysDeptRequest request) {
        return R.ok(sysDeptService.create(request));
    }

    @Operation(summary = "修改部门", description = "父部门不能是自己或子部门；父级变化会同步子孙祖级链")
    @PreAuthorize("@ps.has('system:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysDeptRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysDeptService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除部门", description = "存在子部门或部门下有用户时不允许删除")
    @PreAuthorize("@ps.has('system:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        request.getIds().forEach(sysDeptService::delete);
        return R.ok();
    }
}
