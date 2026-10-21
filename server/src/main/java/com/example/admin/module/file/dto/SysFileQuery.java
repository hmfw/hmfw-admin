package com.example.admin.module.file.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SysFileQuery {

    @Min(1)
    private long pageNum = 1;

    @Min(1)
    @Max(500)
    private long pageSize = 10;

    /** 按原始文件名模糊搜索 */
    private String originalName;

    /** 按 MIME 类型匹配；以 / 结尾时按前缀匹配，如 image/ */
    private String mimeType;
}
