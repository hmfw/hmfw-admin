package com.example.admin.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
@TableName("sys_notice_target")
public class SysNoticeTarget {
    @TableId
    private Long noticeId;
    private String targetType;
    private Long targetId;
}
