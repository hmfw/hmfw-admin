package com.example.admin.module.dict.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典「类型 + 字典项」一体化保存请求。
 * 一个事务里存类型与其全部字典项：类型本身新增或修改，字典项按 id 做增（无 id）/改（有 id）/删（原有但本次未带）。
 * 字典项的 dictType 由类型统一派生，无需逐项上送。
 */
@Data
public class DictTypeSaveRequest {

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

    /** 该类型下的全部字典项（最终态；缺失的视为删除） */
    @Valid
    private List<Item> dataList = new ArrayList<>();

    @Data
    public static class Item {

        /** 已有项带 id（更新），新增项为空 */
        private Long id;

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
}
