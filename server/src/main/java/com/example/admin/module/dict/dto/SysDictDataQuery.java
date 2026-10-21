package com.example.admin.module.dict.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysDictDataQuery {

    @Min(value = 1, message = "从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "至少 1 条")
    @Max(value = 500, message = "最多 500 条")
    private long pageSize = 10;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    private String dictLabel;
    private Integer status;
}
