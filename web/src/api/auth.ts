import { http } from './request'
import type {
  CaptchaVO,
  LoginRequest,
  LoginResponse,
  MenuTreeNode,
  Profile,
  UpdateProfileRequest,
  UserInfo,
} from '@/types/api'

export function login(data: LoginRequest) {
  return http.post<LoginResponse>('/auth/login', data)
}

/** 图形验证码（enabled=false 时前端跳过验证码输入） */
export function getCaptcha() {
  return http.get<CaptchaVO>('/auth/captcha')
}

export function logout() {
  return http.post<void>('/auth/logout')
}

export function getMe() {
  return http.get<UserInfo>('/auth/me')
}

/** 当前用户可见的菜单树，供侧边栏动态渲染 */
export function getMenus() {
  return http.get<MenuTreeNode[]>('/auth/menus')
}

/** 个人资料（含邮箱/手机号，供个人中心页回显） */
export function getProfile() {
  return http.get<Profile>('/auth/profile')
}

/** 修改个人资料（昵称/邮箱/手机号） */
export function updateProfile(data: UpdateProfileRequest) {
  return http.post<void>('/auth/profile', data)
}

/** 修改当前用户密码（需原密码） */
export function updatePassword(oldPassword: string, newPassword: string) {
  return http.post<void>('/auth/password', { oldPassword, newPassword })
}
