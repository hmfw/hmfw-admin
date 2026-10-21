import { http } from './request'
import type {
  LoginLogQuery,
  OperLogQuery,
  PageResult,
  SysLoginLog,
  SysOperLog,
} from '@/types/api'

/* ------------------------------ 操作日志 ------------------------------ */
export function pageOperLogs(query: OperLogQuery) {
  return http.get<PageResult<SysOperLog>>('/monitor/operlog/list', { ...query })
}

export function deleteOperLogs(ids: string[]) {
  return http.post<void>('/monitor/operlog/delete', { ids })
}

export function cleanOperLogs() {
  return http.post<void>('/monitor/operlog/clean')
}

/* ------------------------------ 登录日志 ------------------------------ */
export function pageLoginLogs(query: LoginLogQuery) {
  return http.get<PageResult<SysLoginLog>>('/monitor/loginlog/list', { ...query })
}

export function deleteLoginLogs(ids: string[]) {
  return http.post<void>('/monitor/loginlog/delete', { ids })
}

export function cleanLoginLogs() {
  return http.post<void>('/monitor/loginlog/clean')
}
