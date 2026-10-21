package com.example.admin.module.notice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MessageQuery {
    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;
    @Min(value = 1, message = "至少 1 条")
    @Max(value = 100, message = "最多 100 条")
    private long pageSize = 10;
    private String title;
    private Integer noticeType;
    /** 0 未读 1 已读 */
    private Integer readStatus;
}
