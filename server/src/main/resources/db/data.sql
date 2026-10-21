-- ============================================================
-- 种子数据
--
-- 用 H2 的 MERGE ... KEY 写入，幂等：每次启动都会执行，
-- 只会把「内置数据」重置为初始值，你新建的用户/角色/菜单不受影响。
-- 副作用是：改了 admin 密码后重启会恢复成 admin123 —— 对开发环境来说
-- 反而是防锁死的保险，正式上线请自行替换这套种子机制。
--
-- 所有账号密码统一为 admin123
--   admin  = 超级管理员，权限 "*:*:*"，拥有全部接口权限
--   viewer = 只读用户，仅有各 list 权限，用于验证 403 越权拦截
-- ============================================================

-- ---------- 用户 ----------
MERGE INTO sys_user (id, username, password, nickname, email, phone, dept_id, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (1, 'admin', '$2y$10$lN34P4XiAHY4BX44XF4.9Ouewvbz4wOZ3J1C4.3ixv7uU5vbP5NL.',
            '超级管理员', 'admin@example.com', '13800000001', 1, 1, '内置账号，勿删', NOW(), NOW(), 0);

MERGE INTO sys_user (id, username, password, nickname, email, phone, dept_id, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (2, 'viewer', '$2y$10$lN34P4XiAHY4BX44XF4.9Ouewvbz4wOZ3J1C4.3ixv7uU5vbP5NL.',
            '只读用户', 'viewer@example.com', '13800000002', 2, 1, '仅用于越权测试', NOW(), NOW(), 0);

-- ---------- 部门 ----------
MERGE INTO sys_dept (id, parent_id, ancestors, dept_name, leader, phone, email, sort, status, create_time, update_time, deleted)
    KEY (id)
    VALUES (1, 0, '0', '总公司', '管理员', '13800000001', 'hq@example.com', 1, 1, NOW(), NOW(), 0);

MERGE INTO sys_dept (id, parent_id, ancestors, dept_name, leader, phone, email, sort, status, create_time, update_time, deleted)
    KEY (id)
    VALUES (2, 1, '0,1', '研发部', '张三', '13800000003', 'dev@example.com', 1, 1, NOW(), NOW(), 0);

MERGE INTO sys_dept (id, parent_id, ancestors, dept_name, leader, phone, email, sort, status, create_time, update_time, deleted)
    KEY (id)
    VALUES (3, 1, '0,1', '运维部', '李四', '13800000004', 'ops@example.com', 2, 1, NOW(), NOW(), 0);

-- ---------- 角色 ----------
MERGE INTO sys_role (id, role_name, role_key, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (1, '超级管理员', 'admin', 1, 1, '拥有所有权限', NOW(), NOW(), 0);

MERGE INTO sys_role (id, role_name, role_key, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (2, '只读用户', 'readonly', 2, 1, '只有查询权限', NOW(), NOW(), 0);

-- ---------- 菜单 ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (1, 0, '系统管理', 'M', '/system', NULL, NULL, 'setting', 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (2, 1, '用户管理', 'C', '/system/user', 'system/user/index', 'system:user:list', 'user', 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (3, 2, '用户查询', 'F', NULL, NULL, 'system:user:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (4, 2, '用户新增', 'F', NULL, NULL, 'system:user:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (5, 2, '用户修改', 'F', NULL, NULL, 'system:user:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (6, 2, '用户删除', 'F', NULL, NULL, 'system:user:remove', NULL, 4, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (7, 2, '重置密码', 'F', NULL, NULL, 'system:user:resetPwd', NULL, 5, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (8, 1, '角色管理', 'C', '/system/role', 'system/role/index', 'system:role:list', 'peoples', 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (9, 8, '角色查询', 'F', NULL, NULL, 'system:role:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (10, 8, '角色新增', 'F', NULL, NULL, 'system:role:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (11, 8, '角色修改', 'F', NULL, NULL, 'system:role:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (12, 8, '角色删除', 'F', NULL, NULL, 'system:role:remove', NULL, 4, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (13, 1, '菜单管理', 'C', '/system/menu', 'system/menu/index', 'system:menu:list', 'tree', 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (14, 13, '菜单查询', 'F', NULL, NULL, 'system:menu:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (15, 13, '菜单新增', 'F', NULL, NULL, 'system:menu:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (16, 13, '菜单修改', 'F', NULL, NULL, 'system:menu:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (17, 13, '菜单删除', 'F', NULL, NULL, 'system:menu:remove', NULL, 4, 1, 1, NOW(), NOW());

-- ---------- 系统监控 ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (18, 0, '系统监控', 'M', '/monitor', NULL, NULL, 'monitor', 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (19, 18, '操作日志', 'C', '/monitor/operlog', 'monitor/operlog/index', 'monitor:operlog:list', 'log', 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (20, 18, '登录日志', 'C', '/monitor/loginlog', 'monitor/loginlog/index', 'monitor:loginlog:list', 'logininfor', 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (21, 19, '操作日志删除', 'F', NULL, NULL, 'monitor:operlog:remove', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (22, 19, '操作日志清空', 'F', NULL, NULL, 'monitor:operlog:clean', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (23, 20, '登录日志删除', 'F', NULL, NULL, 'monitor:loginlog:remove', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (24, 20, '登录日志清空', 'F', NULL, NULL, 'monitor:loginlog:clean', NULL, 2, 1, 1, NOW(), NOW());

-- ---------- 部门管理（挂在系统管理下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (25, 1, '部门管理', 'C', '/system/dept', 'system/dept/index', 'system:dept:list', 'dept', 4, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (26, 25, '部门查询', 'F', NULL, NULL, 'system:dept:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (27, 25, '部门新增', 'F', NULL, NULL, 'system:dept:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (28, 25, '部门修改', 'F', NULL, NULL, 'system:dept:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (29, 25, '部门删除', 'F', NULL, NULL, 'system:dept:remove', NULL, 4, 1, 1, NOW(), NOW());

-- ---------- 字典管理（挂在系统管理下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (30, 1, '字典管理', 'C', '/system/dict', 'system/dict/index', 'system:dict:list', 'dict', 5, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (31, 30, '字典查询', 'F', NULL, NULL, 'system:dict:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (32, 30, '字典新增', 'F', NULL, NULL, 'system:dict:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (33, 30, '字典修改', 'F', NULL, NULL, 'system:dict:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (34, 30, '字典删除', 'F', NULL, NULL, 'system:dict:remove', NULL, 4, 1, 1, NOW(), NOW());

-- ---------- 文件管理（挂在系统管理下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (35, 1, '文件管理', 'C', '/system/file', 'system/file/index', 'system:file:list', 'upload', 6, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (36, 35, '文件查询', 'F', NULL, NULL, 'system:file:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (37, 35, '文件上传', 'F', NULL, NULL, 'system:file:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (38, 35, '文件删除', 'F', NULL, NULL, 'system:file:remove', NULL, 3, 1, 1, NOW(), NOW());

-- ---------- 用户导入/导出按钮权限（挂在用户管理下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (39, 2, '用户导入', 'F', NULL, NULL, 'system:user:import', NULL, 6, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (40, 2, '用户导出', 'F', NULL, NULL, 'system:user:export', NULL, 7, 1, 1, NOW(), NOW());

-- ---------- 通知公告（挂在系统管理下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (41, 1, '通知公告', 'C', '/system/notice', 'system/notice/index', 'system:notice:list', 'notice', 7, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (42, 41, '公告查询', 'F', NULL, NULL, 'system:notice:query', NULL, 1, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (43, 41, '公告新增', 'F', NULL, NULL, 'system:notice:add', NULL, 2, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (44, 41, '公告修改', 'F', NULL, NULL, 'system:notice:edit', NULL, 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (45, 41, '公告删除', 'F', NULL, NULL, 'system:notice:remove', NULL, 4, 1, 1, NOW(), NOW());

-- ---------- 在线用户（挂在系统监控下） ----------
MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (46, 18, '在线用户', 'C', '/monitor/online', 'monitor/online/index', 'monitor:online:list', 'user', 3, 1, 1, NOW(), NOW());

MERGE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, create_time, update_time)
    KEY (id)
    VALUES (47, 46, '强制下线', 'F', NULL, NULL, 'monitor:online:kick', NULL, 1, 1, 1, NOW(), NOW());

-- ---------- 字典样例数据 ----------
MERGE INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (1, '用户性别', 'sys_user_sex', 1, '用户性别列表', NOW(), NOW(), 0);

MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (1, 'sys_user_sex', '男', '0', 1, 1, NULL, NOW(), NOW(), 0);

MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (2, 'sys_user_sex', '女', '1', 2, 1, NULL, NOW(), NOW(), 0);

MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (3, 'sys_user_sex', '未知', '2', 3, 1, NULL, NOW(), NOW(), 0);

-- 系统开关状态（正常/停用）
MERGE INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (2, '系统开关', 'sys_normal_disable', 1, '正常/停用状态列表', NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (4, 'sys_normal_disable', '正常', '0', 1, 1, NULL, NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (5, 'sys_normal_disable', '停用', '1', 2, 1, NULL, NOW(), NOW(), 0);

-- 系统是否（是/否）
MERGE INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (3, '系统是否', 'sys_yes_no', 1, '是/否布尔列表', NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (6, 'sys_yes_no', '是', 'Y', 1, 1, NULL, NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (7, 'sys_yes_no', '否', 'N', 2, 1, NULL, NOW(), NOW(), 0);

-- 显示状态（显示/隐藏）
MERGE INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (4, '显示状态', 'sys_show_hide', 1, '显示/隐藏状态列表', NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (8, 'sys_show_hide', '显示', '1', 1, 1, NULL, NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (9, 'sys_show_hide', '隐藏', '0', 2, 1, NULL, NOW(), NOW(), 0);

-- 登录结果（成功/失败）
MERGE INTO sys_dict_type (id, dict_name, dict_type, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (5, '登录结果', 'sys_login_result', 1, '登录日志成功/失败状态', NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (10, 'sys_login_result', '成功', '0', 1, 1, NULL, NOW(), NOW(), 0);
MERGE INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status, remark, create_time, update_time, deleted)
    KEY (id)
    VALUES (11, 'sys_login_result', '失败', '1', 2, 1, NULL, NOW(), NOW(), 0);

-- ---------- 用户角色关联 ----------
MERGE INTO sys_user_role (user_id, role_id) KEY (user_id, role_id) VALUES (1, 1);
MERGE INTO sys_user_role (user_id, role_id) KEY (user_id, role_id) VALUES (2, 2);

-- ---------- 角色菜单关联 ----------
-- admin 角色：全部菜单
MERGE INTO sys_role_menu (role_id, menu_id) KEY (role_id, menu_id)
    SELECT 1, id FROM sys_menu;

-- readonly 角色：只给目录和列表页，不给任何按钮权限 —— 所以它调不了写接口
MERGE INTO sys_role_menu (role_id, menu_id) KEY (role_id, menu_id)
    SELECT 2, id FROM sys_menu WHERE id IN (1, 2, 8, 13);
