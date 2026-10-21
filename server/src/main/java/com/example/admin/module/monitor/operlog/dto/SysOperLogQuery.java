package com.example.admin.module.monitor.operlog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SysOperLogQuery {

    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "至少 1 条")
    @Max(value = 500, message = "最多 500 条")
    private long pageSize = 10;

    /** 模块标题模糊匹配 */
    private String title;

    /** 操作人账号模糊匹配 */
    private String operName;

    /** 0 正常 1 异常 */
    private Integer status;
}
