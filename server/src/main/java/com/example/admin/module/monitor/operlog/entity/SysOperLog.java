package com.example.admin.module.monitor.operlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块标题 */
    private String title;

    /** 业务类型：OTHER/INSERT/UPDATE/DELETE/CLEAN */
    private String businessType;

    /** 请求的类方法 */
    private String method;

    /** HTTP 方法 */
    private String requestMethod;

    /** 操作人账号 */
    private String operName;

    /** 请求地址 */
    private String operUrl;

    /** 操作 IP */
    private String operIp;

    /** 请求参数 */
    private String operParam;

    /** 返回结果 */
    private String jsonResult;

    /** 0 正常 1 异常 */
    private Integer status;

    /** 异常信息 */
    private String errorMsg;

    /** 耗时(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
