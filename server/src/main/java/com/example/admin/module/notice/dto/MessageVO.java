package com.example.admin.module.notice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String noticeTitle;
    private Integer noticeType;
    private String noticeContent;
    private String creatorName;
    private LocalDateTime publishTime;
    private Integer readStatus;
    private LocalDateTime readTime;
}
