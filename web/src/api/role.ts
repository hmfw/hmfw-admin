import { http } from './request'
import type { OptionItem, PageResult, RoleQuery, RoleRequest, SysRole } from '@/types/api'

export function pageRoles(query: RoleQuery) {
  return http.get<PageResult<SysRole>>('/system/role/list', { ...query })
}

/** 角色字典：启用角色的 label/value 选项，供下拉选择使用 */
export function getRoleOptions() {
  return http.get<OptionItem[]>('/system/role/options')
}

export function getRole(id: string) {
  return http.get<SysRole>('/system/role/info', { id })
}

export function createRole(data: RoleRequest) {
  return http.post<string>('/system/role', data)
}

export function updateRole(id: string, data: RoleRequest) {
  return http.post<void>('/system/role/update', { id, ...data })
}

export function deleteRole(id: string) {
  return http.post<void>('/system/role/delete', { ids: [id] })
}

export function assignRoleMenus(id: string, menuIds: string[]) {
  return http.post<void>('/system/role/assignMenus', { id, menuIds })
}
