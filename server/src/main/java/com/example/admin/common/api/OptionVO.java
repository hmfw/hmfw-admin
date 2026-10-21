package com.example.admin.common.api;

import lombok.Data;

/**
 * 通用下拉字典项，字段与前端 Select 的 option 保持一致。
 * value 为不透明字符串码值（可承载字符串主键、雪花 id、字典编码等），不参与运算。
 */
@Data
public class OptionVO {

    /** 展示文本 */
    private String label;

    /** 选项值（字符串码值） */
    private String value;

    public static OptionVO of(String label, String value) {
        OptionVO vo = new OptionVO();
        vo.setLabel(label);
        vo.setValue(value);
        return vo;
    }
}
