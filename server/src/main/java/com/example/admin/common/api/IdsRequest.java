package com.example.admin.common.api;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量 id 请求体，用于删除等按 id 操作的接口。
 */
@Data
public class IdsRequest {

    @NotEmpty(message = "不能为空")
    private List<Long> ids;
}
