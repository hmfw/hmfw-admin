package com.example.admin.module.notice.controller;

import com.example.admin.common.api.IdsRequest;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.R;
import com.example.admin.module.notice.dto.MessageQuery;
import com.example.admin.module.notice.dto.MessageVO;
import com.example.admin.module.notice.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人消息")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;

    @Operation(summary = "我的消息")
    @GetMapping("/list")
    public R<PageResult<MessageVO>> list(@Valid MessageQuery query) {
        return R.ok(service.page(query));
    }

    @Operation(summary = "消息详情，打开后自动已读")
    @GetMapping("/info")
    public R<MessageVO> info(@RequestParam Long id) {
        return R.ok(service.detail(id));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(service.unreadCount());
    }

    @Operation(summary = "标记已读")
    @PostMapping("/read")
    public R<Void> read(@Valid @RequestBody IdsRequest request) {
        service.markRead(request.getIds());
        return R.ok();
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public R<Void> readAll() {
        service.markAllRead();
        return R.ok();
    }
}
