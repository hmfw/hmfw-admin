package com.example.admin.module.monitor.operlog.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.monitor.operlog.dto.SysOperLogQuery;
import com.example.admin.module.monitor.operlog.entity.SysOperLog;
import com.example.admin.module.monitor.operlog.service.SysOperLogService;
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

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/monitor/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService sysOperLogService;

    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ps.has('monitor:operlog:list')")
    @GetMapping("/list")
    public R<PageResult<SysOperLog>> page(@Valid SysOperLogQuery query) {
        return R.ok(sysOperLogService.page(query));
    }

    @Operation(summary = "删除操作日志", description = "按 id 批量删除")
    @PreAuthorize("@ps.has('monitor:operlog:remove')")
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        sysOperLogService.delete(request.getIds());
        return R.ok();
    }

    @Operation(summary = "清空操作日志")
    @PreAuthorize("@ps.has('monitor:operlog:clean')")
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PostMapping("/clean")
    public R<Void> clean() {
        sysOperLogService.clean();
        return R.ok();
    }
}
