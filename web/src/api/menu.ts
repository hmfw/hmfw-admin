import { http } from './request'
import type { MenuDetail, MenuRequest, MenuTreeNode } from '@/types/api'

export function getMenuTree(menuName?: string, status?: number) {
  return http.get<MenuTreeNode[]>('/system/menu/tree', { menuName, status })
}

export function getMenu(id: string) {
  return http.get<MenuDetail>('/system/menu/info', { id })
}

export function createMenu(data: MenuRequest) {
  return http.post<string>('/system/menu', data)
}

export function updateMenu(id: string, data: MenuRequest) {
  return http.post<void>('/system/menu/update', { id, ...data })
}

export function deleteMenu(id: string) {
  return http.post<void>('/system/menu/delete', { ids: [id] })
}
