package com.example.admin.module.monitor.loginlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 登录 IP */
    private String ip;

    /** 0 成功 1 失败 */
    private Integer status;

    /** 提示信息 */
    private String msg;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
