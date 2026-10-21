import { http } from './request'
import type {
  PageResult,
  SysUser,
  UserCreateRequest,
  UserImportResult,
  UserQuery,
  UserUpdateRequest,
} from '@/types/api'

export function pageUsers(query: UserQuery) {
  return http.get<PageResult<SysUser>>('/system/user/list', { ...query })
}

export function getUser(id: string) {
  return http.get<SysUser>('/system/user/info', { id })
}

export function createUser(data: UserCreateRequest) {
  return http.post<string>('/system/user', data)
}

export function updateUser(id: string, data: UserUpdateRequest) {
  return http.post<void>('/system/user/update', { id, ...data })
}

export function deleteUser(id: string) {
  return http.post<void>('/system/user/delete', { ids: [id] })
}

export function resetPassword(id: string, password: string) {
  return http.post<void>('/system/user/resetPwd', { id, password })
}

export function updateUserStatus(id: string, status: number) {
  return http.post<void>('/system/user/changeStatus', { id, status })
}

export function assignUserRoles(id: string, roleIds: string[]) {
  return http.post<void>('/system/user/assignRoles', { id, roleIds })
}

/** Excel 导入用户 */
export function importUsers(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<UserImportResult>('/system/user/import', formData)
}

/** 导出用户 Excel（返回 Blob） */
export function exportUsers(query: UserQuery) {
  return http.getBlob('/system/user/export', { ...query })
}

/** 下载导入模板（返回 Blob） */
export function downloadTemplate() {
  return http.getBlob('/system/user/importTemplate')
}
