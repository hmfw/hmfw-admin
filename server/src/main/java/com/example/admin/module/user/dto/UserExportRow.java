package com.example.admin.module.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 用户导出行模型（EasyExcel 写入用）。
 */
@Data
@ColumnWidth(20)
public class UserExportRow {

    @ExcelProperty("用户名")
    @ColumnWidth(15)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(15)
    private String nickname;

    @ExcelProperty("部门")
    @ColumnWidth(15)
    private String deptName;

    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty("邮箱")
    @ColumnWidth(25)
    private String email;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("创建时间")
    @ColumnWidth(20)
    private String createTime;
}
