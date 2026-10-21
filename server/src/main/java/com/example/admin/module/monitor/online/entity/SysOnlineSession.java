package com.example.admin.module.monitor.online.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_online_session")
public class SysOnlineSession {

    @TableId
    private String id;
    private Long userId;
    private String username;
    private String nickname;
    private String ip;
    private String userAgent;
    private LocalDateTime loginTime;
    private LocalDateTime lastActiveTime;
    private LocalDateTime expiresTime;
    private Integer status;

    @TableField(exist = false)
    private Boolean current;
}
