package com.example.admin.module.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDictDataRequest {

    /** 新增为空，修改必填 */
    private Long id;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "最长 100")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(max = 100, message = "最长 100")
    private String dictValue;

    private Integer sort = 0;

    private Integer status = 1;

    @Size(max = 255, message = "最长 255")
    private String remark;
}
