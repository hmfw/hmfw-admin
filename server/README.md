# Server

基于 Spring Boot 的后台管理系统后端，标准 RBAC 权限模型，纯 REST API。

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | Temurin LTS |
| Spring Boot | 3.5.16 | |
| Spring Security | 6.5.11 | 随 Boot 引入 |
| MyBatis-Plus | 3.5.17 | 含分页插件 |
| jjwt | 0.13.0 | JWT 签发与校验 |
| springdoc-openapi | 2.8.17 | 在线接口文档 |
| H2 | 2.3.232 | 文件数据库，随应用启动 |
| Lombok | — | 随 Boot 引入 |

## 快速开始

```bash
# 启动（首次会自动建库建表并写入种子数据）
mvn spring-boot:run
```

启动后：

- 服务地址 <http://localhost:8080>
- 接口文档 <http://localhost:8080/swagger-ui.html>

### 内置账号

| 账号 | 密码 | 角色 | 权限 |
|---|---|---|---|
| `admin` | `admin123` | 超级管理员 | `*:*:*`，全部接口 |
| `viewer` | `admin123` | 只读用户 | 仅各资源的 `list` 权限，用于验证越权拦截 |

### 调用示例

```bash
# 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

# 调受保护接口
curl -s "http://localhost:8080/api/system/user/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```

## 接口一览

> 实时接口文档以 Swagger UI（<http://localhost:8080/swagger-ui.html>）为准；本节侧重**权限标识**速查——各接口所需的 `perms` 不在 Swagger 中体现。

统一返回 `{ code, msg, data, timestamp }`，业务码 200 成功 / 400 参数错 / 401 未认证 / 403 无权限 / 1001+ 业务错误。

> 接口风格：查询用 `GET`，写操作统一 `POST`；不使用 `PUT`/`DELETE` 与 `/{id}` 路径参数。
> 详情用 `GET .../info?id=`，修改的 `id` 放请求体，删除用 `POST .../delete` + body `{"ids":[...]}`（支持批量）。
> 所有 **id 字段以字符串返回**（装箱 `Long` 统一序列化为 `String`，规避前端 JS 大整数精度丢失；见 `config/JacksonConfig`）；`total`、`timestamp`、`expiresIn` 等计数/时间戳仍是数字。入参 id 传字符串即可，Jackson 会转回 `Long`。

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/captcha` | 图形验证码（base64 图 + key；`enabled=false` 时跳过） | 匿名 |
| POST | `/login` | 登录，返回 JWT（按 IP 限流，可带验证码） | 匿名 |
| POST | `/logout` | 登出并注销当前服务端会话 | 登录 |
| POST | `/refresh` | 凭当前有效 token 换新 token（滑动续期） | 登录 |
| GET | `/me` | 当前用户信息 + 角色 + 权限集合 | 登录 |
| GET | `/menus` | 当前用户可见菜单树，供前端动态路由 | 登录 |
| GET | `/profile` | 个人资料（含邮箱/手机号） | 登录 |
| POST | `/profile` | 修改个人资料（昵称/邮箱/手机号） | 登录 |
| POST | `/password` | 修改本人密码（校验原密码） | 登录 |

> 其余模块接口（权限标识）：
> - 部门 `/api/system/dept`：`system:dept:list/query/add/edit/remove`
> - 字典 `/api/system/dict/type`、`/api/system/dict/data`：`system:dict:list/query/add/edit/remove`（`/type/save` 一体化保存类型+字典项，需 add 或 edit；`/data/type/{dictType}` 仅需登录）
> - 操作日志 `/api/monitor/operlog`：`monitor:operlog:list/remove/clean`
> - 登录日志 `/api/monitor/loginlog`：`monitor:loginlog:list/remove/clean`
> - 在线用户 `/api/monitor/online`：`monitor:online:list/kick`

### 首页与个人消息

- `GET /api/dashboard/summary`：用户、登录、在线会话、文件、未读消息、7 日趋势和最近操作
- `GET /api/message/list`、`/info`、`/unread-count`：当前用户可见消息与未读状态
- `POST /api/message/read`、`/read-all`：标记单条/批量或全部已读
- 通知公告支持按全员、角色、部门或用户指定接收范围

### 用户 `/api/system/user`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/list` | 分页查询 | `system:user:list` |
| GET | `/info?id=` | 详情（含已分配角色 id） | `system:user:query` |
| POST | `` | 新增 | `system:user:add` |
| POST | `/update` | 修改（body 带 id，不含账号名与密码） | `system:user:edit` |
| POST | `/delete` | 逻辑删除（body `{"ids":[...]}`） | `system:user:remove` |
| POST | `/resetPwd` | 重置密码 | `system:user:resetPwd` |
| POST | `/changeStatus` | 启用/停用 | `system:user:edit` |
| POST | `/assignRoles` | 分配角色 | `system:user:edit` |

### 角色 `/api/system/role`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/list` | 分页查询 | `system:role:list` |
| GET | `/options` | 角色字典（启用角色的 label/value 选项，下拉用） | `system:role:list` |
| GET | `/info?id=` | 详情（含已分配菜单 id） | `system:role:query` |
| POST | `` | 新增 | `system:role:add` |
| POST | `/update` | 修改（body 带 id） | `system:role:edit` |
| POST | `/delete` | 删除（body `{"ids":[...]}`） | `system:role:remove` |
| POST | `/assignMenus` | 分配菜单权限 | `system:role:edit` |

### 菜单 `/api/system/menu`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/tree` | 菜单树，可按名称/状态过滤 | `system:menu:list` |
| GET | `/info?id=` | 详情 | `system:menu:query` |
| POST | `` | 新增 | `system:menu:add` |
| POST | `/update` | 修改（body 带 id，父菜单不能是自身或子孙） | `system:menu:edit` |
| POST | `/delete` | 删除（body `{"ids":[...]}`，有子节点或被角色引用时拒绝） | `system:menu:remove` |

## 权限模型

经典 RBAC 五表：`sys_user` / `sys_role` / `sys_menu` / `sys_user_role` / `sys_role_menu`。
扩展表：`sys_dept`（部门树）、`sys_role_dept`（自定义数据范围）、`sys_dict_type`/`sys_dict_data`（字典）、`sys_oper_log`/`sys_login_log`（日志）。

数据权限：`sys_role.data_scope`（1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人），由 `security/datascope/DataScopeService` 取当前用户各角色并集，应用于用户列表查询（超级管理员不受限）。

菜单表同时承担两个职责：

- `menu_type = M/C` —— 目录和菜单，决定前端能看到哪些页面（`/auth/menus` 返回）
- `menu_type = F` —— 按钮，只提供 `perms` 权限标识，不参与路由

接口鉴权用注解表达，超级管理员通过 `*:*:*` 通配放行：

```java
@PreAuthorize("@ps.has('system:user:list')")
@PreAuthorize("@ps.hasAny('system:user:edit', 'system:user:add')")
```

权限判断是细粒度的 —— 例如 `viewer` 被授了「角色管理」菜单但没被授「角色查询」按钮，因此可以 `GET /api/system/role/list` 列表，却不能 `GET /api/system/role/info?id=` 详情。

## 项目结构

```
com.example.admin
├── common/       统一响应 R / 分页 PageResult / 批量 IdsRequest / 业务码 ResultCode / 业务异常 BizException / 全局异常处理
│                 log（@Log + LogAspect 操作日志切面）/ ratelimit（@RateLimit + 切面，按 IP 限流）
├── config/       Security / MyBatis-Plus（分页插件）/ 字段自动填充（MyMetaObjectHandler）/ Jackson
├── security/     JwtUtils、JWT 过滤器、LoginUser、UserDetailsServiceImpl、权限校验 bean（PermissionService，名为 ps）、
│                 401 入口点与 403 处理器、JSON 响应写出、SecurityUtils、datascope（数据范围计算）
└── module/
    ├── auth/     登录认证、个人中心（profile/改密）、刷新 token、图形验证码（captcha）
    ├── user/     用户（含部门归属与数据范围过滤）
    ├── role/     角色（含 data_scope 与自定义部门）
    ├── menu/     菜单
    ├── dept/     部门（树 + ancestors 祖级链）
    ├── dict/     字典类型 + 字典数据
    ├── notice/   通知公告 + 接收范围 + 个人消息已读状态
    ├── dashboard/ 首页统计摘要与趋势
    └── monitor/  操作日志 + 登录日志 + 在线会话/强制下线

resources/
├── application.properties
├── db/schema.sql     建表（CREATE TABLE IF NOT EXISTS，幂等）
├── db/data.sql       种子数据（MERGE，幂等；34 条菜单权限 + 部门/字典样例）
└── mapper/*.xml      手写 JOIN 查询（SysMenuMapper / SysRoleMapper）
```

> 各 `module` 的 service 是具体类，不做接口/实现分离；entity 用 `@TableLogic` 实现逻辑删除。手写 SQL（`mapper/*.xml`）需自行带 `status`/`deleted` 条件，`@TableLogic` 只对 MyBatis-Plus 生成的 CRUD 生效。

## 数据库

使用 **H2 文件库**，数据落在 `./data/admin.mv.db`，随应用启动，无需安装任何数据库服务。

### 重置数据

```bash
# 停掉应用，删目录，重启即可重建
rm -rf data/
mvn spring-boot:run
```

### 关于种子数据

`data.sql` 用 `MERGE ... KEY` 写入，每次启动都会执行。效果是：

- 你新建的用户、角色、菜单**不受影响**
- 内置的 `admin` / `viewer` 账号和内置菜单权限会**被重置为初始状态**（包括密码恢复为 `admin123`）

对开发环境来说这反而是防锁死的保险。正式上线时请替换这套种子机制。

### 换成 MySQL

代码本身不绑定 H2 —— MyBatis-Plus 的方言由 `admin.mybatis.db-type` 配置切换，SQL 也是 MySQL 风格写的。要切换：

1. `pom.xml` 加回 `com.mysql:mysql-connector-j`（scope `runtime`）
2. `application.properties` 把 `spring.datasource.*` 指向你的 MySQL，并把 `admin.mybatis.db-type` 改为 `MYSQL`
3. `db/schema.sql` 里的 `CREATE TABLE IF NOT EXISTS` 可直接在 MySQL 执行

## 测试

```bash
mvn test
```

88 个用例，跑在 H2 内存库上，无需外部依赖。覆盖：

| 测试类 | 用例数 | 覆盖点 |
|---|---|---|
| `AdminApplicationTests` | 1 | Spring 上下文能正常加载 |
| `SchemaSmokeTest` | 3 | 全部表与种子数据完整性 —— 防止建表脚本写错 |
| `SeedPasswordTest` | 2 | BCrypt 密文能匹配 `admin123` |
| `JwtUtilsTest` | 7 | 签发/解析/过期/篡改/换密钥/弱密钥 |
| `CaptchaServiceTest` | 4 | 验证码生成/一次性校验/过期/开关 |
| `RateLimiterTest` | 3 | 固定窗口限流计数/独立 key/窗口重置 |
| `AuthApiTest` | 14 | 登录成功失败、401、权限集合、验证码公开、个人资料/改密、刷新 token |
| `SysUserApiTest` | 10 | 分页、CRUD、逻辑删除、唯一约束、密码不外泄 |
| `SysRoleMenuApiTest` | 12 | 菜单树嵌套与过滤、权限分配、删除保护、细粒度越权 |
| `SysDeptApiTest` | 6 | 部门树/CRUD/删除保护 + 数据范围端到端 |
| `SysDictApiTest` | 7 | 字典类型/数据 CRUD、一体化保存（类型+项增改删）、唯一约束、越权 |
| `SysMonitorLogApiTest` | 4 | 操作日志（切面落库）、登录日志、越权 |
| `DashboardMessageOnlineApiTest` | 4 | 看板、定向消息/已读状态、强制下线、登出失效 |

> 共 88 个用例。API 测试的公共基类 `ApiTestBase`（提供 `adminToken()` / `viewerToken()`）本身不含用例。

API 测试通过 MockMvc 走**真实过滤器链**，401/403 和 `@PreAuthorize` 都是真跑一遍。

## 安全提示

上线前务必修改：

- `admin.jwt.secret` —— 换成足够长的随机串（至少 32 字节）
- 内置账号的默认密码
- `SecurityConfig` 中的 CORS 配置（当前放开为 `*`）
- 验证码与限流默认开启（`admin.captcha.enabled` / `admin.ratelimit.enabled`）；验证码答案与限流计数当前为**进程内存**，多实例部署需换 Redis
