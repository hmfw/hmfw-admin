package com.example.admin.module.notice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SysNoticeRequest {

    private Long id;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题最长 200 个字符")
    private String noticeTitle;

    @NotNull(message = "公告类型不能为空")
    @Min(value = 1, message = "公告类型不正确")
    @Max(value = 2, message = "公告类型不正确")
    private Integer noticeType;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 100000, message = "公告内容过长")
    private String noticeContent;

    @NotNull(message = "公告状态不能为空")
    @Min(value = 0, message = "公告状态不正确")
    @Max(value = 1, message = "公告状态不正确")
    private Integer status;

    /** ALL/ROLE/DEPT/USER */
    @NotBlank(message = "请选择接收范围")
    private String audienceType = "ALL";

    private List<Long> targetIds;

    @Size(max = 255, message = "备注最长 255 个字符")
    private String remark;
}
