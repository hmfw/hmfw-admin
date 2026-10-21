package com.example.admin.module.monitor.loginlog.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.monitor.loginlog.dto.SysLoginLogQuery;
import com.example.admin.module.monitor.loginlog.entity.SysLoginLog;
import com.example.admin.module.monitor.loginlog.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录日志")
@RestController
@RequestMapping("/api/monitor/loginlog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService sysLoginLogService;

    @Operation(summary = "分页查询登录日志")
    @PreAuthorize("@ps.has('monitor:loginlog:list')")
    @GetMapping("/list")
    public R<PageResult<SysLoginLog>> page(@Valid SysLoginLogQuery query) {
        return R.ok(sysLoginLogService.page(query));
    }

    @Operation(summary = "删除登录日志", description = "按 id 批量删除")
    @PreAuthorize("@ps.has('monitor:loginlog:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        sysLoginLogService.delete(request.getIds());
        return R.ok();
    }

    @Operation(summary = "清空登录日志")
    @PreAuthorize("@ps.has('monitor:loginlog:clean')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @PostMapping("/clean")
    public R<Void> clean() {
        sysLoginLogService.clean();
        return R.ok();
    }
}
