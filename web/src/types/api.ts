// 与后端 DTO 一一对应的类型定义。字段名与 Spring Boot 返回的 JSON 保持一致。

/** 统一响应封装 { code, msg, data, timestamp } */
export interface R<T> {
  code: number
  msg: string
  data: T
  timestamp: number
}

/** 分页响应体 */
export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 下拉字典项，与 @hmfw/ant-design Select 的 option 结构一致 */
export interface OptionItem {
  label: string
  value: string
}

/** 当前登录用户信息（/api/auth/me） */
export interface UserInfo {
  userId: string
  username: string
  nickname: string
  avatarUrl?: string | null
  roles: string[]
  /** 权限标识；超级管理员含 "*:*:*" */
  permissions: string[]
}

/** 登录响应（/api/auth/login） */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

/** 登录请求（/api/auth/login） */
export interface LoginRequest {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

/** 图形验证码（GET /api/auth/captcha） */
export interface CaptchaVO {
  enabled: boolean
  captchaKey?: string
  captchaImage?: string
}

/** 修改个人资料请求（/api/auth/profile） */
export interface UpdateProfileRequest {
  nickname?: string
  email?: string
  phone?: string
}

/** 个人资料回显（GET /api/auth/profile） */
export interface Profile {
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatarUrl?: string | null
}

/** 当前用户头像写入结果 */
export interface AvatarResult {
  avatarUrl: string
}

/** 菜单树节点（/api/auth/menus 与 /api/system/menu/tree 共用结构） */
export interface MenuTreeNode {
  id: string
  parentId: string
  menuName: string
  /** M=目录 C=菜单 F=按钮 */
  menuType: 'M' | 'C' | 'F'
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  children: MenuTreeNode[]
}

/** 菜单详情 */
export interface MenuDetail {
  id: string
  parentId: string
  menuName: string
  menuType: 'M' | 'C' | 'F'
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
  createTime?: string
}

export interface MenuRequest {
  parentId: string
  menuName: string
  menuType: 'M' | 'C' | 'F'
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
}

/** 用户 */
export interface SysUser {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  deptId?: string
  deptName?: string
  status: number
  remark?: string
  createTime?: string
  /** 列表接口返回的有效角色名称 */
  roleNames?: string[]
  /** 仅详情接口填充 */
  roleIds?: string[]
}

export interface UserQuery {
  pageNum: number
  pageSize: number
  username?: string
  status?: number
  deptId?: string
}

export interface UserCreateRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  deptId?: string
  status?: number
  remark?: string
  roleIds?: string[]
}

export interface UserUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  deptId?: string
  status?: number
  remark?: string
  roleIds?: string[]
}

/** 角色 */
export interface SysRole {
  id: string
  roleName: string
  roleKey: string
  sort?: number
  dataScope?: number
  status: number
  remark?: string
  createTime?: string
  /** 仅详情接口填充 */
  menuIds?: string[]
  /** 仅详情接口填充：自定义数据范围的部门 id */
  deptIds?: string[]
}

export interface RoleQuery {
  pageNum: number
  pageSize: number
  roleName?: string
  status?: number
}

export interface RoleRequest {
  roleName: string
  roleKey: string
  sort?: number
  dataScope?: number
  status?: number
  remark?: string
  menuIds?: string[]
  deptIds?: string[]
}

/** 部门 */
export interface SysDept {
  id: string
  parentId: string
  ancestors?: string
  deptName: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: number
  createTime?: string
}

/** 部门树节点 */
export interface DeptTreeNode {
  id: string
  parentId: string
  deptName: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: number
  createTime?: string
  children: DeptTreeNode[]
}

export interface DeptRequest {
  parentId: string
  deptName: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: number
}

/** 字典类型 */
export interface SysDictType {
  id: string
  dictName: string
  dictType: string
  status: number
  remark?: string
  createTime?: string
}

export interface DictTypeQuery {
  pageNum: number
  pageSize: number
  dictName?: string
  dictType?: string
  status?: number
}

export interface DictTypeRequest {
  dictName: string
  dictType: string
  status?: number
  remark?: string
}

/** 字典数据 */
export interface SysDictData {
  id: string
  dictType: string
  dictLabel: string
  dictValue: string
  sort?: number
  status: number
  remark?: string
  createTime?: string
}

export interface DictDataQuery {
  pageNum: number
  pageSize: number
  dictType: string
  dictLabel?: string
  status?: number
}

export interface DictDataRequest {
  dictType: string
  dictLabel: string
  dictValue: string
  sort?: number
  status?: number
  remark?: string
}

/** 字典项（一体化保存用；id 为空表示新增行） */
export interface DictDataItem {
  id?: string
  dictLabel: string
  dictValue: string
  sort: number
  status: number
  remark?: string
}

/** 字典「类型 + 字典项」一体化保存请求 */
export interface DictTypeSaveRequest {
  id?: string
  dictName: string
  dictType: string
  status: number
  remark?: string
  dataList: DictDataItem[]
}

/** 操作日志 */
export interface SysOperLog {
  id: string
  title?: string
  businessType?: string
  method?: string
  requestMethod?: string
  operName?: string
  operUrl?: string
  operIp?: string
  operParam?: string
  jsonResult?: string
  status: number
  errorMsg?: string
  costTime: number
  operTime?: string
}

export interface OperLogQuery {
  pageNum: number
  pageSize: number
  title?: string
  operName?: string
  status?: number
}

/** 登录日志 */
export interface SysLoginLog {
  id: string
  username?: string
  ip?: string
  status: number
  msg?: string
  loginTime?: string
}

export interface LoginLogQuery {
  pageNum: number
  pageSize: number
  username?: string
  status?: number
}

/** 文件 */
export interface SysFile {
  id: string
  originalName: string
  url: string
  size: number
  mimeType?: string
  uploaderName?: string
  uploadTime?: string
}

export interface FileQuery {
  pageNum: number
  pageSize: number
  originalName?: string
  mimeType?: string
}

/** 通知公告 */
export interface SysNotice {
  id: string
  noticeTitle: string
  /** 1 通知 2 公告 */
  noticeType: number
  /** HTML 正文 */
  noticeContent: string
  /** 0 草稿 1 已发布 */
  status: number
  /** ALL/ROLE/DEPT/USER */
  audienceType: string
  targetIds: string[]
  creatorId?: string
  creatorName?: string
  publishTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface NoticeQuery {
  pageNum: number
  pageSize: number
  noticeTitle?: string
  noticeType?: number
  status?: number
}

export interface NoticeRequest {
  noticeTitle: string
  noticeType: number
  noticeContent: string
  status: number
  /** ALL/ROLE/DEPT/USER */
  audienceType: string
  targetIds?: string[]
  remark?: string
}

export interface NoticeTargetOptions {
  roles: OptionItem[]
  departments: OptionItem[]
  users: OptionItem[]
}

export interface MessageItem {
  id: string
  noticeTitle: string
  noticeType: number
  noticeContent: string
  creatorName?: string
  publishTime?: string
  readStatus: number
  readTime?: string
}

export interface MessageQuery {
  pageNum: number
  pageSize: number
  title?: string
  noticeType?: number
  readStatus?: number
}

export interface LoginTrend {
  date: string
  success: number
  failure: number
}

export interface DashboardActivity {
  title?: string
  businessType?: string
  operator?: string
  status: number
  time?: string
}

export interface DashboardSummary {
  totalUsers: number
  enabledUsers: number
  todayLogins: number
  todayFailures: number
  onlineSessions: number
  fileCount: number
  fileSize: number
  unreadMessages: number
  loginTrend: LoginTrend[]
  recentActivities: DashboardActivity[]
  recentMessages: MessageItem[]
}

export interface OnlineSession {
  id: string
  userId: string
  username: string
  nickname?: string
  ip?: string
  userAgent?: string
  loginTime: string
  lastActiveTime: string
  expiresTime: string
  status: number
  current: boolean
}

export interface OnlineSessionQuery {
  pageNum: number
  pageSize: number
  username?: string
  ip?: string
}

/** Excel 导入结果 */
export interface UserImportError {
  row: number
  username?: string
  message: string
}

export interface UserImportResult {
  successCount: number
  failCount: number
  errors: UserImportError[]
}
