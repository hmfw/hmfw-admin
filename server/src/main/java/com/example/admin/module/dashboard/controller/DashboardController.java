package com.example.admin.module.dashboard.controller;

import com.example.admin.common.api.R;
import com.example.admin.module.dashboard.dto.DashboardSummaryVO;
import com.example.admin.module.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @Operation(summary = "首页统计摘要")
    @GetMapping("/summary")
    public R<DashboardSummaryVO> summary() {
        return R.ok(service.summary());
    }
}
