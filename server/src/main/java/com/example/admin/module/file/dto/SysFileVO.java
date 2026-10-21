package com.example.admin.module.file.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysFileVO {

    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** 访问路径 */
    private String url;

    /** 文件大小(字节) */
    private Long size;

    /** MIME 类型 */
    private String mimeType;

    /** 上传者昵称 */
    private String uploaderName;

    /** 上传时间 */
    private LocalDateTime uploadTime;
}
