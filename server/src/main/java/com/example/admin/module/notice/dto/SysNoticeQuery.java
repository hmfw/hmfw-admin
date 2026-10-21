package com.example.admin.module.notice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SysNoticeQuery {

    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "至少 1 条")
    @Max(value = 500, message = "最多 500 条")
    private long pageSize = 10;

    private String noticeTitle;
    private Integer noticeType;
    private Integer status;
}
