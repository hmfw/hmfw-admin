package com.example.admin.module.monitor.online.controller;

import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.monitor.online.dto.SysOnlineSessionQuery;
import com.example.admin.module.monitor.online.entity.SysOnlineSession;
import com.example.admin.module.monitor.online.service.SysOnlineSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "在线用户")
@RestController
@RequestMapping("/api/monitor/online")
@RequiredArgsConstructor
public class SysOnlineSessionController {

    private final SysOnlineSessionService service;

    @Operation(summary = "在线会话列表")
    @PreAuthorize("@ps.has('monitor:online:list')")
    @GetMapping("/list")
    public R<PageResult<SysOnlineSession>> list(@Valid SysOnlineSessionQuery query) {
        return R.ok(service.page(query));
    }

    @Operation(summary = "强制会话下线")
    @PreAuthorize("@ps.has('monitor:online:kick')")
    @Log(title = "在线用户", businessType = BusinessType.UPDATE)
    @PostMapping("/kick")
    public R<Void> kick(@Valid @RequestBody SessionIdsRequest request) {
        service.kick(request.getIds());
        return R.ok();
    }

    @Data
    public static class SessionIdsRequest {
        @NotEmpty(message = "至少选择一条会话")
        private List<String> ids;
    }
}
