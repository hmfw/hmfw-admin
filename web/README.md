# web

后台管理系统前端：基于 **Vue 3 + TypeScript + Vite** 的单页应用，为 `server/`（Spring Boot RBAC 后端）提供管理界面。所有数据通过 `/api` 与后端交互，涵盖登录鉴权、用户 / 角色 / 菜单的 RBAC 管理。

## 技术栈

- **Vue 3**（`<script setup>` SFC）+ **TypeScript**
- **Vite 8** 构建与开发服务器
- **vue-router 5**（history 模式，静态路由 + 全局守卫）
- **@hmfw/ant-design** / **@hmfw/icons** —— 自定义发布的 Vue 3 UI 组件与图标库（注意：不是 `ant-design-vue`，所有组件 / 图标都从这两个库导入）
- 包管理器 **pnpm**，Node 24

状态管理未使用 Pinia，而是一个模块级 `reactive` 单例（见下文 `stores/auth.ts`）。

## 环境要求

- Node 24
- pnpm
- 后端 `server/` 需在 `http://localhost:8080` 运行（开发期由 Vite 代理 `/api` 转发）

## 命令

```bash
pnpm install
pnpm dev        # 启动 Vite 开发服务器（:5173），代理 /api -> http://localhost:8080
pnpm build      # vue-tsc -b && vite build（类型检查是构建的一部分）
pnpm preview    # 本地预览生产构建产物
pnpm test       # Vitest 单元测试（jsdom，一次性运行）
pnpm test:watch # Vitest 监听模式
pnpm lint       # eslint . --fix
pnpm format     # prettier --write src/
```

> 单元测试用 Vitest，覆盖请求层（`api/request.ts`）、auth store、icon 工具等核心逻辑。

## 代码风格

- Prettier：不加分号、单引号、宽度 100
- 路径别名 `@` → `src/`（见 `vite.config.ts` 与 `tsconfig`）
- ESLint 扁平配置，`skipFormatting` 放在最后，样式规则全部交给 Prettier

## 目录结构

```
src/
├── api/              # 请求层
│   ├── request.ts    # 唯一 fetch 封装：注入 Bearer、解包 .data、抛出 BizError、滑动续期
│   ├── auth.ts       # 登录 / 登出 / 当前用户 / 菜单树 / 验证码 / 个人资料 / 刷新 token
│   ├── user.ts       # 用户管理接口
│   ├── role.ts       # 角色管理接口（含数据范围）
│   ├── menu.ts       # 菜单管理接口
│   ├── dept.ts       # 部门管理接口
│   ├── dict.ts       # 字典（类型 + 数据）接口
│   ├── log.ts        # 操作日志 / 登录日志接口
│   ├── dashboard.ts  # 首页统计接口
│   ├── message.ts    # 个人消息与已读状态
│   └── online.ts     # 在线会话与强制下线
├── stores/
│   └── auth.ts       # reactive 单例（非 Pinia），缓存用户信息 + 菜单 + 权限判断
├── router/
│   └── index.ts      # 静态路由 + 全局守卫（鉴权 / 细粒度权限 / 标题）
├── layout/
│   └── BasicLayout.vue  # 后台整体布局，由菜单树渲染侧边栏与面包屑
├── views/            # 页面
│   ├── Login.vue     # 登录（含图形验证码）
│   ├── Dashboard.vue
│   ├── MessageView.vue # 个人消息中心
│   ├── Profile.vue   # 个人中心（改资料 / 改密码）
│   ├── NotFound.vue
│   ├── system/       # UserView / RoleView / MenuView / DeptView / DictView
│   └── monitor/      # OperLogView / LoginLogView / OnlineView
├── types/
│   └── api.ts        # 与后端 DTO 一一对应的类型定义
├── utils/
│   └── icon.ts       # 后端 icon 字符串 → @hmfw/icons 组件的映射
├── main.ts           # 应用入口（挂载 router）
└── style.css
```

## 请求层约定（`src/api/request.ts`）

- `BASE_URL` 为相对路径 `/api`；生产环境（nginx 等）必须同样把 `/api` 代理到后端。
- 从 `localStorage['admin_token']` 读取 token，自动注入 `Authorization: Bearer <token>`。
- 后端所有响应都是统一信封 `{ code, msg, data, timestamp }`；请求层解包返回 `.data`。
- `code !== 200` 时抛出带类型的 `BizError`，并自动 `message.error` 弹窗（传 `silent` 可关闭）。
- 收到 HTTP `401` 时清除 token 并跳转登录页（登录页本身不重复跳转）。
- 对外只暴露 `http.get(path, params?, silent?)` 与 `http.post(path, body?, silent?)`，各 api 文件在其上做薄封装。
- **所有 id 一律按 `string` 收发**（与后端一致：后端把 `Long` id 序列化为字符串以规避 JS 大整数精度丢失）。`src/types/api.ts` 里 `id`/`parentId`/`roleIds`/`menuIds`/`OptionItem.value` 等均为 `string`；而 `total`、分页页码等计数仍是 `number`。

新增接口的步骤：在 `src/api/*.ts` 中加一个 `http.get`/`http.post` 薄封装，并在 `src/types/api.ts` 中补上与后端 DTO 对应的类型。

## 接口与后端的 API 约定

与后端保持一致（详见根目录 `CLAUDE.md`）：

- 查询用 `GET`，**所有写操作用 `POST`**；没有 `PUT`/`DELETE`，也没有 `/{id}` 路径参数。
- 详情：`GET .../info?id=`；更新：id 放进请求体；删除：`POST .../delete`，body 为 `{ "ids": [...] }`（批量）。

当前已接入的接口见 `src/api/`（`auth` / `user` / `role` / `menu`），均为对后端的薄封装；后端接口与权限详见 `server/README.md` 或 Swagger UI。

## 鉴权与权限

- **登录态**：token 存于 `localStorage['admin_token']`，由 `request.ts` 读写。
- **auth store（`stores/auth.ts`）**：模块级 `reactive` 单例，通过 `useAuthStore()` 使用，缓存 `userInfo`、`menus`，并提供 `loadUserInfo()` / `reset()` / `hasPermission()`。首次进入受守卫路由（以及刷新页面后）会并发拉取 `/auth/me` 与 `/auth/menus`，由 `loaded` 标志驱动。
- **路由守卫（`router/index.ts`）**：
  - `meta.public` 路由（登录页、404）放行；已登录再访问登录页会跳回首页。
  - 无 token → 跳转 `/login` 并带 `redirect`。
  - `loaded` 为空时补拉用户信息，失败则重置并回登录页。
  - 路由 `meta.perm` 存在且 `hasPermission()` 不通过 → 拦回 `/dashboard`。
- **权限判断**：`hasPermission(perm)` 对拥有 `*:*:*` 的超级管理员恒为 `true`。
- 前端的权限控制仅是 UX（隐藏入口 / 拦截导航），**后端注解鉴权才是权威**。

## 菜单树与路由的关系（关键）

前端路由是 `router/index.ts` 里的**静态**定义；后端返回的菜单树（`GET /auth/menus`）只用于渲染侧边栏 / 面包屑并控制可见性。`BasicLayout.vue` 会过滤掉 `F`（按钮）节点和 `visible=0` 的节点，再把菜单树转成侧边栏。

菜单节点的 `path` 必须与前端路由路径**完全一致**（它被用作菜单项的 key 并与 `route.path` 比较）。因此新增一个页面需要两处同时改动：

1. 在 `router/index.ts` 中新增路由定义；
2. 在后端新增对应菜单行（通过「菜单管理」页面或 `server/db/data.sql`）。

菜单表的 `component` 字段目前**不**用于动态生成路由。

图标方面：后端 `icon` 字段是字符串（如 `setting` / `user` / `peoples`），由 `utils/icon.ts` 映射到 `@hmfw/icons` 组件，未命中回退 `BarsOutlined`。

## 生产部署

- `pnpm build` 产物在 `dist/`。
- 部署时（nginx 等）必须把 `/api` 反向代理到后端服务，路径规则与开发期 Vite 代理保持一致。
