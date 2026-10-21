package com.example.admin.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 单条用户导入失败明细。 */
@Data
@AllArgsConstructor
public class UserImportError {

    private int row;
    private String username;
    private String message;
}
