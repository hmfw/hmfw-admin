package com.example.admin.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice_read")
public class SysNoticeRead {
    @TableId
    private Long noticeId;
    private Long userId;
    private LocalDateTime readTime;
}
