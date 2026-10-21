package com.example.admin.module.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户导入结果。
 */
@Data
public class UserImportResult {

    private int successCount;

    private int failCount;

    private List<UserImportError> errors;
}
