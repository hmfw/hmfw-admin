package com.example.admin.module.notice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String noticeTitle;
    private Integer noticeType;
    private String noticeContent;
    private Integer status;
    private Long creatorId;
    private String creatorName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishTime;
    private String remark;

    @TableField(exist = false)
    private String audienceType;

    @TableField(exist = false)
    private List<Long> targetIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
