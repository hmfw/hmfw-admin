package com.example.admin.module.notice.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.common.log.BusinessType;
import com.example.admin.common.log.Log;
import com.example.admin.module.notice.dto.SysNoticeQuery;
import com.example.admin.module.notice.dto.SysNoticeRequest;
import com.example.admin.module.notice.dto.NoticeTargetOptionsVO;
import com.example.admin.module.notice.entity.SysNotice;
import com.example.admin.module.notice.service.SysNoticeService;
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

@Tag(name = "通知公告")
@RestController
@RequestMapping("/api/system/notice")
@RequiredArgsConstructor
public class SysNoticeController {

    private final SysNoticeService sysNoticeService;

    @Operation(summary = "分页查询通知公告")
    @PreAuthorize("@ps.has('system:notice:list')")
    @GetMapping("/list")
    public R<PageResult<SysNotice>> list(@Valid SysNoticeQuery query) {
        return R.ok(sysNoticeService.page(query));
    }

    @Operation(summary = "通知公告详情")
    @PreAuthorize("@ps.has('system:notice:query')")
    @GetMapping("/info")
    public R<SysNotice> info(@RequestParam Long id) {
        return R.ok(sysNoticeService.getDetail(id));
    }

    @Operation(summary = "公告接收对象选项")
    @PreAuthorize("@ps.hasAny('system:notice:add', 'system:notice:edit')")
    @GetMapping("/target-options")
    public R<NoticeTargetOptionsVO> targetOptions() {
        return R.ok(sysNoticeService.targetOptions());
    }

    @Operation(summary = "新增通知公告")
    @PreAuthorize("@ps.has('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> create(@Valid @RequestBody SysNoticeRequest request) {
        return R.ok(sysNoticeService.create(request));
    }

    @Operation(summary = "修改通知公告")
    @PreAuthorize("@ps.has('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysNoticeRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        sysNoticeService.update(request.getId(), request);
        return R.ok();
    }

    @Operation(summary = "删除通知公告")
    @PreAuthorize("@ps.has('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsRequest request) {
        sysNoticeService.delete(request.getIds());
        return R.ok();
    }
}
