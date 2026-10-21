import { http } from './request'
import type { DeptRequest, DeptTreeNode, OptionItem, SysDept } from '@/types/api'

export function getDeptTree(params?: { deptName?: string; status?: number }) {
  return http.get<DeptTreeNode[]>('/system/dept/tree', { ...params })
}

/** 部门字典：启用部门的 label/value，供下拉选择 */
export function getDeptOptions() {
  return http.get<OptionItem[]>('/system/dept/options')
}

export function getDept(id: string) {
  return http.get<SysDept>('/system/dept/info', { id })
}

export function createDept(data: DeptRequest) {
  return http.post<string>('/system/dept', data)
}

export function updateDept(id: string, data: DeptRequest) {
  return http.post<void>('/system/dept/update', { id, ...data })
}

export function deleteDept(id: string) {
  return http.post<void>('/system/dept/delete', { ids: [id] })
}
