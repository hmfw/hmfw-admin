# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作时提供指导。

## 仓库结构

两个相互独立的项目，根目录没有统一的构建工具或包管理器：

- `server/` —— Spring Boot 3.5 / Java 21 后端，一个仅提供 REST 接口的 RBAC 管理 API，底层使用 H2 文件数据库。采用 Maven（无 wrapper；请使用系统的 `mvn`）。
- `web/` —— Vue 3 + TypeScript + Vite 单页应用；即管理后台前端。采用 pnpm。

`web` 只通过 `/api` 与 `server` 通信。所有代码注释和文档均为中文——编辑时请保持这一风格。

## 命令

### server/（Java 21，Maven）

```bash
mvn spring-boot:run                        # 在 :8080 启动；首次运行会创建并初始化数据库
mvn test                                   # 88 个测试，基于 H2 内存库，无需外部服务
mvn -Dtest=SysUserApiTest test             # 运行单个测试类
mvn -Dtest=SysUserApiTest#testMethod test  # 运行单个测试方法
mvn clean package                          # 构建 jar
rm -rf data/                               # 重置数据库（先停止应用；下次启动会重新创建并初始化）
```

- Swagger UI：<http://localhost:8080/swagger-ui.html>（公开放行）
- 内置账号：`admin`/`admin123`（超级管理员，`*:*:*`），`viewer`/`admin123`（仅列表权限，用于验证 403）
- `spring-boot-devtools` 处于启用状态：重新编译会触发上下文重启。

### web/（Node 24，pnpm）

```bash
pnpm install
pnpm dev      # Vite 在 :5173；将 /api 代理到 http://localhost:8080（后端必须已启动）
pnpm build    # vue-tsc -b && vite build（类型检查是构建的一部分）
pnpm test     # Vitest 单元测试（jsdom）；pnpm test:watch 为监听模式
pnpm lint     # eslint . --fix
pnpm format   # prettier --write src/
```

前端使用 Vitest 做单元测试（覆盖请求层、auth store、icon 工具）。代码风格：Prettier —— 不加分号、使用单引号、宽度 100；路径别名 `@` → `src/`。ESLint 扁平配置将 `skipFormatting` 放在最后，使 Prettier 掌管所有样式规则。

## API 约定（后端与前端必须保持一致）

- 查询使用 `GET`；**所有**写操作使用 `POST`。没有 `PUT`/`DELETE`，也没有 `/{id}` 路径参数。
- 详情：`GET .../info?id=`；更新：id 放在请求体中；删除：`POST .../delete`，请求体 `{"ids":[...]}`（批量）。
- 每个响应都是统一信封 `{ code, msg, data, timestamp }`（`common/api/R.java`）。`code` 200 = 成功，400/401/403 对应 HTTP 状态，1001+ = 业务错误。401/403/500 处理器同样会写出该信封，因此前端始终以 JSON 解析。
- 前端 `src/api/request.ts` 是唯一的请求层：从 `localStorage['admin_token']` 注入 `Bearer` 令牌，解包 `.data`，并在 `code !== 200` 时抛出带类型的 `BizError`（除非设置 `silent`，否则会自动 `message.error`）。新增接口时，在 `src/api/*.ts` 中添加薄封装的 `http.get`/`http.post`，并在 `src/types/api.ts` 中添加对应类型（与后端 DTO 保持镜像）。

## 权限模型

RBAC 五张核心表：`sys_user` / `sys_role` / `sys_menu` / `sys_user_role` / `sys_role_menu`。
另有：`sys_dept`（部门树）、`sys_role_dept`（自定义数据范围）、`sys_dict_type`/`sys_dict_data`（字典）、
`sys_oper_log`/`sys_login_log`（日志）。

- `sys_menu.menu_type`：`M`/`C` = 目录/菜单（驱动导航），`F` = 按钮——仅携带一个 `perms` 字符串。
- 后端鉴权通过注解表达：`@PreAuthorize("@ps.has('system:user:list')")`（或 `@ps.hasAny(...)`），解析到名为 `ps` 的 `PermissionService` Bean。`admin` 角色的 key 在加载时被注入 `*:*:*`（`UserDetailsServiceImpl`），从而短路所有校验。
- 前端与之对应：路由携带 `meta.perm`，路由守卫在缺少权限时重定向到 `/dashboard`；`auth.hasPermission()` 将 `*:*:*` 视为全部放行。隐藏按钮仅为交互体验——后端校验才是权威。
- **数据权限**：`sys_role.data_scope`（1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人），由 `security/datascope/DataScopeService` 按当前用户所有角色取并集，应用到用户列表查询（超级管理员不受限）。

## 菜单树 vs. 路由（关键坑点）

前端路由在 `src/router/index.ts` 中是**静态**的；后端菜单树（`GET /api/auth/menus`）仅用于渲染侧边栏/面包屑并控制可见性。菜单节点的 `path` 必须与前端路由路径完全一致（它被用作 Menu 的 `key`，并与 `route.path` 比较）。因此新增页面需要两处同时改动：路由中的路由定义**以及**后端的菜单行（通过「菜单管理」或 `db/data.sql`）。菜单表的 `component` 字段当前未用于动态路由生成。

## 后端结构与规则

包根 `com.example.admin`：

- `common/` —— `R`、`PageResult`、`IdsRequest`、`ResultCode`、`BizException`、`GlobalExceptionHandler`；`log/`（`@Log` + `LogAspect` 操作日志切面）、`ratelimit/`（`@RateLimit` + 切面，按 IP 限流）
- `config/` —— 安全配置（JWT + 持久化在线会话、BCrypt、CORS `*`、方法级安全）、MyBatis-Plus、字段自动填充、Jackson
- `security/` —— `JwtUtils` + 过滤器、`LoginUser`、`PermissionService`（`ps`）、`UserDetailsServiceImpl`、`datascope/DataScopeService`（数据范围）
- `module/{auth,user,role,menu,dept,dict,notice,dashboard,monitor}/` —— 各模块按需包含 `controller` / `service` / `mapper` / `entity` / `dto`；`notice` 含定向接收与个人已读状态，`dashboard` 提供首页摘要，`monitor` 含日志和在线会话管理

需要保持的规则：

- `*/mapper/*.xml`（与 Mapper 接口同级目录）中的手写 SQL 必须自行包含 `status`/`deleted` 条件——`@TableLogic` 仅对 MyBatis-Plus 生成的 CRUD 生效。参见 `SysMenuMapper.xml` 中的注释。
- 分页方言来自 `admin.mybatis.db-type`（默认 `H2`）；`PaginationInnerInterceptor` 需要显式的 `DbType`。最大页大小 500，超出则返回空。
- 种子数据 `db/data.sql` 使用 `MERGE`，并在**每次**启动时运行：用户创建的行会保留，但内置的 `admin`/`viewer` 账号及其菜单分配会被重置为初始状态（密码恢复为 `admin123`）。这是有意为之的防锁死保护。
- `db/schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，具有幂等性；H2 以 MySQL 模式运行（`MODE=MySQL`）。切换到 MySQL 的方法记录在 `server/README.md` 中。
- 测试运行在共享的 `schema.sql`/`data.sql` 上（`src/test/resources/application-test.properties`，H2 内存库），因此测试与生产之间的 SQL 不会出现分歧。API 测试通过 MockMvc 走真实的安全过滤器链——`ApiTestBase` 提供 `adminToken()` / `viewerToken()` 辅助方法；用 `viewerToken()` 来断言 403。

## 前端结构

- `stores/auth.ts` **不是** Pinia——它是一个模块级的 `reactive` 单例，通过 `useAuthStore()` 暴露。它在首次受守卫的导航时（以及刷新后）惰性加载 `/auth/me` + `/auth/menus`；由 `loaded` 驱动。
- `layout/BasicLayout.vue` 将后端菜单树转换为侧边栏项（过滤掉 `F` 节点和 `visible=0`），并从同一棵树派生面包屑。
- `utils/icon.ts` 将后端的图标名称字符串（`setting`、`user`、`peoples` 等）映射到 `@hmfw/icons` 组件；未映射的名称回退到 `BarsOutlined`。
- UI 组件来自 `@hmfw/ant-design`，图标来自 `@hmfw/icons`——这是自定义发布的 Vue 3 库（不是 `ant-design-vue`）。所有内容都从这两个库导入，而非其他 antd 包。
- Vite 开发服务器将 `/api` 代理到 `:8080`；生产环境（nginx 等）必须以同样方式代理 `/api`——`request.ts` 中的 `BASE_URL` 是相对路径 `/api`。

## 安全说明（在 server/README.md 中已标注）

在任何非开发环境部署之前：修改 `admin.jwt.secret`（≥32 字节，否则 jjwt 会抛出 `WeakKeyException`），修改内置密码，并收紧 `SecurityConfig` 中的 CORS `*`。
