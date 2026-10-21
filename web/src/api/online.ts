import { http } from './request'
import type { OnlineSession, OnlineSessionQuery, PageResult } from '@/types/api'

export function pageOnlineSessions(query: OnlineSessionQuery) {
  return http.get<PageResult<OnlineSession>>('/monitor/online/list', { ...query })
}

export function kickSessions(ids: string[]) {
  return http.post<void>('/monitor/online/kick', { ids })
}
