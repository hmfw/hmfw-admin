package com.example.admin.module.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 用户导入行模型（EasyExcel 读取用）。
 */
@Data
@ColumnWidth(20)
public class UserImportRow {

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
    private String status; // 正常/停用

    @ExcelProperty("角色")
    @ColumnWidth(15)
    private String roleName;
}
