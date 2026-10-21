# Tasks

## 1. 扩展用户头像数据契约

- [x] 1.1 在 `SchemaSmokeTest` 和认证 API 测试中增加已有数据库头像列、空头像兼容及 profile/me 返回头像字段的失败用例，运行 `./mvnw test -Dtest=SchemaSmokeTest,AuthApiTest` 确认新断言先失败
- [x] 1.2 为 `sys_user` 增加可空头像资源键的建表定义与幂等迁移，并扩展 `SysUser`、`LoginUser`、用户 VO 和认证 DTO 映射，运行 `./mvnw test -Dtest=SchemaSmokeTest,AuthApiTest` 验证新旧用户契约通过

## 2. 实现服务端头像存储与接口

- [x] 2.1 新增头像服务测试，覆盖 5 MiB 上限、JPEG/PNG 内容识别、损坏文件、超大像素、512×512 PNG 规范化、路径约束、替换失败回滚和旧文件清理，运行目标测试确认实现前失败
- [x] 2.2 实现专用头像存储服务，将规范化文件写入 `${admin.upload.path}/avatars` 并协调用户关联更新与清理，运行头像服务测试验证全部存储和失败一致性场景通过
- [x] 2.3 在 `AuthApiTest` 增加当前用户上传、替换、移除、无文件管理权限仍可上传、未登录写入被拒绝的接口测试，并增加公开头像读取、固定内容类型、nosniff、immutable 缓存及非法资源键测试，运行目标测试确认接口实现前失败
- [x] 2.4 实现 `POST /api/auth/avatar`、`DELETE /api/auth/avatar` 与 `GET /api/profile-avatars/{key}`，配置只读资源鉴权例外并更新服务端 API 文档，运行 `./mvnw test -Dtest=AuthApiTest` 验证写入权限、资源响应和 DTO 同步通过

## 3. 建立前端共享头像与裁剪基础

- [x] 3.1 使用现有包管理器加入 `cropperjs` 2.x 并锁定版本，运行依赖安装与 `npm run build` 验证 Vue/TypeScript 构建兼容
- [x] 3.2 扩展 `avatar.spec.ts` 和布局测试，覆盖自定义图片优先、空地址字符回退及图片加载错误回退，运行相关 Vitest 用例确认共享实现前失败
- [x] 3.3 扩展 API 类型、认证 API 与共享头像展示组件，使个人中心和顶部导航统一消费 `avatarUrl` 并处理图片错误，运行头像工具、store 与布局相关测试验证两处展示一致
- [x] 3.4 为裁剪组件编写选择、方形区域、拖动缩放、512×512 PNG 输出、取消及对象 URL 回收测试，再实现基于 Cropper.js 的异步裁剪模态框，运行组件目标测试验证交互与资源释放

## 4. 接入个人中心头像维护交互

- [x] 4.1 在 `Profile.spec.ts` 增加文件类型和 5 MiB 校验、打开与取消裁剪、上传锁定和首次上传成功同步的失败用例，运行目标测试确认实现前失败
- [x] 4.2 在身份摘要头像附近接入图片选择、裁剪确认和专用上传 API，上传成功后更新本页并刷新全局用户信息，运行 `npm test -- src/views/Profile.spec.ts` 验证首次上传、同步失败提示和重复提交保护
- [x] 4.3 增加替换头像缓存更新、移除确认、移除失败保留、无头像隐藏移除操作及头像操作不改变资料脏状态的测试，再实现相应交互，运行 `npm test -- src/views/Profile.spec.ts src/views/Profile.navigation.spec.ts` 验证全部状态
- [x] 4.4 完善桌面与窄屏下裁剪器、头像操作和加载状态样式，为图标操作补充 tooltip 与可访问名称，并在 `1440x900`、`390x844` 视口检查无重叠、横向溢出或布局跳动

## 5. 集成验证

- [x] 5.1 在 `server` 目录运行 `./mvnw test`，确认数据库迁移、认证、权限与全部既有后端测试通过
- [x] 5.2 在 `web` 目录运行 `npm test` 和 `npm run build`，确认头像、个人中心、导航同步及全部既有前端测试和生产构建通过
- [x] 5.3 使用普通无文件管理权限账号手工验证无头像、JPEG/PNG 首次上传、替换、移除、非法文件、图片加载失败及刷新页面场景，并确认公开头像响应头、旧 URL 失效和磁盘文件生命周期符合设计
