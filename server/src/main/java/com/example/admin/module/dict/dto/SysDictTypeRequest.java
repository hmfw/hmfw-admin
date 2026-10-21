package com.example.admin.module.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDictTypeRequest {

    /** 新增为空，修改必填 */
    private Long id;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "最长 100")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "[a-z0-9_]{2,100}", message = "只能是小写字母、数字、下划线，长度 2-100")
    private String dictType;

    private Integer status = 1;

    @Size(max = 255, message = "最长 255")
    private String remark;
}
