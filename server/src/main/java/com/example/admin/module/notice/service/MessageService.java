package com.example.admin.module.notice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.common.api.PageResult;
import com.example.admin.common.api.ResultCode;
import com.example.admin.common.exception.BizException;
import com.example.admin.module.notice.dto.MessageQuery;
import com.example.admin.module.notice.dto.MessageVO;
import com.example.admin.module.notice.entity.SysNoticeRead;
import com.example.admin.module.notice.mapper.SysNoticeMapper;
import com.example.admin.module.notice.mapper.SysNoticeReadMapper;
import com.example.admin.security.LoginUser;
import com.example.admin.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final SysNoticeMapper noticeMapper;
    private final SysNoticeReadMapper readMapper;

    public PageResult<MessageVO> page(MessageQuery query) {
        LoginUser user = SecurityUtils.getLoginUser();
        Page<MessageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        noticeMapper.selectMessagePage(page, query, user.getUserId(), user.getDeptId());
        return PageResult.of(page);
    }

    @Transactional
    public MessageVO detail(Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        MessageVO message = noticeMapper.selectAccessibleMessage(id, user.getUserId(), user.getDeptId());
        if (message == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        if (message.getReadStatus() == 0) {
            insertRead(id, user.getUserId());
            message.setReadStatus(1);
            message.setReadTime(LocalDateTime.now(ZONE));
        }
        return message;
    }

    public long unreadCount() {
        LoginUser user = SecurityUtils.getLoginUser();
        return noticeMapper.countUnread(user.getUserId(), user.getDeptId());
    }

    @Transactional
    public void markRead(List<Long> ids) {
        LoginUser user = SecurityUtils.getLoginUser();
        for (Long id : ids) {
            if (noticeMapper.selectAccessibleMessage(id, user.getUserId(), user.getDeptId()) != null) {
                insertRead(id, user.getUserId());
            }
        }
    }

    @Transactional
    public void markAllRead() {
        LoginUser user = SecurityUtils.getLoginUser();
        noticeMapper.selectUnreadIds(user.getUserId(), user.getDeptId())
                .forEach(id -> insertRead(id, user.getUserId()));
    }

    private void insertRead(Long noticeId, Long userId) {
        Long exists = readMapper.selectCount(Wrappers.<SysNoticeRead>lambdaQuery()
                .eq(SysNoticeRead::getNoticeId, noticeId)
                .eq(SysNoticeRead::getUserId, userId));
        if (exists > 0) return;
        SysNoticeRead read = new SysNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setReadTime(LocalDateTime.now(ZONE));
        try {
            readMapper.insert(read);
        } catch (DuplicateKeyException ignored) {
            // 并发打开同一条消息时，以数据库唯一键保证幂等。
        }
    }
}
