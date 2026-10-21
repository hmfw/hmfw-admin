# HMFW Admin

一个前后端分离的后台管理系统，内置用户、角色、菜单、部门、字典、通知、文件和系统监控等常用功能。后端采用 Spring Boot，前端采用 Vue 3，基于 RBAC 模型实现菜单、按钮和接口级权限控制。

## 功能

- 登录认证：JWT、图形验证码、令牌刷新、退出登录
- 权限管理：用户、角色、菜单、按钮权限和数据权限
- 组织管理：部门树与部门数据范围
- 系统配置：字典类型和字典数据
- 通知中心：公告发布、定向接收、消息已读状态
- 文件管理：上传、下载、预览和删除
- 系统监控：操作日志、登录日志、在线用户和强制下线
- 工作台：关键数据汇总、趋势和最近操作
- 个人中心：资料维护与密码修改

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5
- Spring Security
- MyBatis-Plus
- JWT
- H2 Database
- springdoc-openapi
- Maven

### 前端

- Vue 3 + TypeScript
- Vite 8
- Vue Router
- `@hmfw/ant-design`
- `@hmfw/icons`
- Vitest
- pnpm

## 项目结构

```text
hmfw-admin/
├── server/    Spring Boot REST API
└── web/       Vue 3 管理后台
```

更详细的说明：

- [后端文档](server/README.md)
- [前端文档](web/README.md)

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 24
- pnpm

开发环境默认使用 H2 文件数据库，无需安装 MySQL、Redis 等外部服务。

## 快速开始

### 1. 启动后端

```bash
cd server
mvn spring-boot:run
```

后端启动后可访问：

- API：<http://localhost:8080/api>
- Swagger UI：<http://localhost:8080/swagger-ui.html>

### 2. 启动前端

另开一个终端：

```bash
cd web
pnpm install
pnpm dev
```

访问 <http://localhost:5173>。

开发服务器会将 `/api` 请求代理到 `http://localhost:8080`。

## 默认账号

| 账号 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | 超级管理员，拥有全部权限 |
| `viewer` | `admin123` | 只读用户，用于验证权限控制 |

默认账号仅用于本地开发和功能演示，部署前必须修改密码。

## 常用命令

### 后端

```bash
cd server
mvn test                  # 运行测试
mvn clean package         # 构建 JAR
mvn spring-boot:run       # 启动开发服务
```

### 前端

```bash
cd web
pnpm dev                  # 启动开发服务
pnpm build                # 类型检查并构建
pnpm test                 # 运行单元测试
pnpm lint                 # 检查并修复代码风格
pnpm format               # 格式化源码
```

## API 约定

- 查询接口使用 `GET`，写操作统一使用 `POST`。
- 响应格式统一为 `{ code, msg, data, timestamp }`。
- 所有 ID 均以字符串形式传递，避免 JavaScript 大整数精度丢失。
- 前端权限控制用于改善交互体验，最终权限以服务端校验为准。

## 数据存储

开发数据默认保存在 `server/data/`。首次启动时会自动建表并写入种子数据。

需要重置本地数据时，先停止后端，然后执行：

```bash
cd server
rm -rf data/
mvn spring-boot:run
```

## 生产部署

部署前至少需要完成以下配置：

- 替换 `admin.jwt.secret`，并通过安全方式管理密钥
- 修改或移除内置账号及默认密码
- 收紧后端 CORS 配置
- 根据部署规模将 H2 切换为 MySQL 等生产数据库
- 多实例部署时，将验证码和限流状态迁移到 Redis 等共享存储
- 配置 Web 服务器将 `/api` 反向代理到后端服务
