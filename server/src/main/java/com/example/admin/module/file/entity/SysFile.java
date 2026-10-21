package com.example.admin.module.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名（uuid+ext） */
    private String storedName;

    /** 访问路径 /upload/yyyyMMdd/uuid.ext */
    private String url;

    /** 磁盘相对路径 */
    private String filePath;

    /** 文件大小(字节) */
    private Long size;

    /** MIME 类型 */
    private String mimeType;

    /** 上传者 ID */
    private Long uploaderId;

    /** 上传者昵称 */
    private String uploaderName;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 0 正常 1 已删除 */
    @TableLogic
    private Integer deleted;
}
