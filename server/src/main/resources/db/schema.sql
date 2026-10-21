-- ============================================================
-- 后台管理系统建表脚本
--
-- 幂等：全部使用 CREATE TABLE IF NOT EXISTS，重启不会清空已有数据。
-- 想彻底重置，停掉应用删掉 data/ 目录再启动即可。
--
-- 数据库为 H2 文件库（MODE=MySQL），因此 DDL 采用 MySQL 风格写法，
-- 保留日后切换到真实 MySQL 的可能。
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密文',
    nickname    VARCHAR(50)           DEFAULT NULL   COMMENT '昵称',
    avatar      VARCHAR(255)          DEFAULT NULL   COMMENT '头像资源键',
    email       VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
    phone       VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    dept_id     BIGINT                DEFAULT NULL   COMMENT '部门 id',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    remark      VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS avatar VARCHAR(255) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_name   VARCHAR(50)  NOT NULL                COMMENT '角色名称',
    role_key    VARCHAR(50)  NOT NULL                COMMENT '角色标识，如 admin',
    sort        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    data_scope  TINYINT      NOT NULL DEFAULT 1      COMMENT '数据范围 1全部 2自定义 3本部门 4本部门及以下 5仅本人',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    remark      VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0      COMMENT '父部门 id，0 为根',
    ancestors   VARCHAR(500)          DEFAULT ''     COMMENT '祖级列表，逗号分隔',
    dept_name   VARCHAR(50)  NOT NULL                COMMENT '部门名称',
    leader      VARCHAR(50)           DEFAULT NULL   COMMENT '负责人',
    phone       VARCHAR(20)           DEFAULT NULL   COMMENT '联系电话',
    email       VARCHAR(100)          DEFAULT NULL   COMMENT '邮箱',
    sort        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_sys_dept_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色 id',
    dept_id BIGINT NOT NULL COMMENT '部门 id',
    PRIMARY KEY (role_id, dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色部门关联表（自定义数据范围）';

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_name   VARCHAR(100) NOT NULL                COMMENT '字典名称',
    dict_type   VARCHAR(100) NOT NULL                COMMENT '字典类型（唯一标识，如 sys_user_sex）',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    remark      VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    dict_type   VARCHAR(100) NOT NULL                COMMENT '字典类型',
    dict_label  VARCHAR(100) NOT NULL                COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL                COMMENT '字典键值',
    sort        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    remark      VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_sys_dict_data_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0      COMMENT '父菜单 id，0 为根',
    menu_name   VARCHAR(50)  NOT NULL                COMMENT '菜单名称',
    menu_type   VARCHAR(1)   NOT NULL                COMMENT 'M 目录 C 菜单 F 按钮',
    path        VARCHAR(200)          DEFAULT NULL   COMMENT '前端路由',
    component   VARCHAR(255)          DEFAULT NULL   COMMENT '前端组件路径',
    perms       VARCHAR(100)          DEFAULT NULL   COMMENT '权限标识，如 system:user:list',
    icon        VARCHAR(100)          DEFAULT NULL   COMMENT '图标',
    sort        INT          NOT NULL DEFAULT 0      COMMENT '排序',
    visible     TINYINT      NOT NULL DEFAULT 1      COMMENT '1 显示 0 隐藏',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '1 启用 0 停用',
    create_time DATETIME     NOT NULL                COMMENT '创建时间',
    update_time DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户 id',
    role_id BIGINT NOT NULL COMMENT '角色 id',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色 id',
    menu_id BIGINT NOT NULL COMMENT '菜单 id',
    PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    title          VARCHAR(100)           DEFAULT NULL   COMMENT '模块标题',
    business_type  VARCHAR(20)            DEFAULT NULL   COMMENT '业务类型：OTHER/INSERT/UPDATE/DELETE/CLEAN',
    method         VARCHAR(200)           DEFAULT NULL   COMMENT '请求的类方法',
    request_method VARCHAR(10)            DEFAULT NULL   COMMENT 'HTTP 方法',
    oper_name      VARCHAR(50)            DEFAULT NULL   COMMENT '操作人账号',
    oper_url       VARCHAR(255)           DEFAULT NULL   COMMENT '请求地址',
    oper_ip        VARCHAR(64)            DEFAULT NULL   COMMENT '操作 IP',
    oper_param     VARCHAR(2000)          DEFAULT NULL   COMMENT '请求参数',
    json_result    VARCHAR(2000)          DEFAULT NULL   COMMENT '返回结果',
    status         TINYINT       NOT NULL DEFAULT 0      COMMENT '0 正常 1 异常',
    error_msg      VARCHAR(2000)          DEFAULT NULL   COMMENT '异常信息',
    cost_time      BIGINT        NOT NULL DEFAULT 0      COMMENT '耗时(ms)',
    oper_time      DATETIME      NOT NULL                COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_sys_oper_log_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(50)          DEFAULT NULL   COMMENT '登录账号',
    ip         VARCHAR(64)          DEFAULT NULL   COMMENT '登录 IP',
    status     TINYINT     NOT NULL DEFAULT 0      COMMENT '0 成功 1 失败',
    msg        VARCHAR(255)         DEFAULT NULL   COMMENT '提示信息',
    login_time DATETIME    NOT NULL                COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_sys_login_log_login_time (login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

CREATE TABLE IF NOT EXISTS sys_file (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name  VARCHAR(255) NOT NULL                COMMENT '原始文件名',
    stored_name    VARCHAR(255) NOT NULL                COMMENT '存储文件名',
    url            VARCHAR(500) NOT NULL                COMMENT '访问路径 /upload/yyyyMMdd/uuid.ext',
    file_path      VARCHAR(500) NOT NULL                COMMENT '磁盘相对路径',
    size           BIGINT       NOT NULL                COMMENT '文件大小(字节)',
    mime_type      VARCHAR(100)          DEFAULT NULL   COMMENT 'MIME类型',
    uploader_id    BIGINT                DEFAULT NULL   COMMENT '上传者ID',
    uploader_name  VARCHAR(64)           DEFAULT NULL   COMMENT '上传者昵称',
    upload_time    DATETIME     NOT NULL                COMMENT '上传时间',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_sys_file_upload_time (upload_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件表';

CREATE TABLE IF NOT EXISTS sys_notice (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    notice_title   VARCHAR(200) NOT NULL                COMMENT '公告标题',
    notice_type    TINYINT      NOT NULL                COMMENT '类型：1通知 2公告',
    notice_content LONGTEXT     NOT NULL                COMMENT '公告正文（HTML）',
    status         TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0草稿 1已发布',
    creator_id     BIGINT                DEFAULT NULL   COMMENT '创建人ID',
    creator_name   VARCHAR(64)           DEFAULT NULL   COMMENT '创建人名称',
    publish_time   DATETIME              DEFAULT NULL   COMMENT '发布时间',
    remark         VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    create_time    DATETIME     NOT NULL                COMMENT '创建时间',
    update_time    DATETIME     NOT NULL                COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_sys_notice_status_time (status, publish_time),
    KEY idx_sys_notice_type (notice_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知公告表';

CREATE TABLE IF NOT EXISTS sys_notice_target (
    notice_id   BIGINT      NOT NULL COMMENT '公告ID',
    target_type VARCHAR(10) NOT NULL COMMENT '接收范围：ALL/ROLE/DEPT/USER',
    target_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '角色/部门/用户ID，全员为0',
    PRIMARY KEY (notice_id, target_type, target_id),
    KEY idx_sys_notice_target_lookup (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告接收范围表';

CREATE TABLE IF NOT EXISTS sys_notice_read (
    notice_id BIGINT   NOT NULL COMMENT '公告ID',
    user_id   BIGINT   NOT NULL COMMENT '用户ID',
    read_time DATETIME NOT NULL COMMENT '阅读时间',
    PRIMARY KEY (notice_id, user_id),
    KEY idx_sys_notice_read_user (user_id, read_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告阅读记录表';

CREATE TABLE IF NOT EXISTS sys_online_session (
    id               VARCHAR(36)  NOT NULL COMMENT 'JWT jti',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    username         VARCHAR(50)  NOT NULL COMMENT '登录账号',
    nickname         VARCHAR(50)           DEFAULT NULL COMMENT '用户昵称',
    ip               VARCHAR(64)           DEFAULT NULL COMMENT '登录IP',
    user_agent       VARCHAR(500)          DEFAULT NULL COMMENT '客户端User-Agent',
    login_time       DATETIME     NOT NULL COMMENT '登录时间',
    last_active_time DATETIME     NOT NULL COMMENT '最后活跃时间',
    expires_time     DATETIME     NOT NULL COMMENT '过期时间',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1在线 0已下线',
    PRIMARY KEY (id),
    KEY idx_sys_online_session_status (status, expires_time),
    KEY idx_sys_online_session_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线会话表';
