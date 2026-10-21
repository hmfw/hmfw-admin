import { http } from './request'
import type {
  NoticeQuery,
  NoticeRequest,
  NoticeTargetOptions,
  PageResult,
  SysNotice,
} from '@/types/api'

export function pageNotices(query: NoticeQuery) {
  return http.get<PageResult<SysNotice>>('/system/notice/list', { ...query })
}

export function getNotice(id: string) {
  return http.get<SysNotice>('/system/notice/info', { id })
}

export function getNoticeTargetOptions() {
  return http.get<NoticeTargetOptions>('/system/notice/target-options')
}

export function createNotice(data: NoticeRequest) {
  return http.post<string>('/system/notice', data)
}

export function updateNotice(id: string, data: NoticeRequest) {
  return http.post<void>('/system/notice/update', { id, ...data })
}

export function deleteNotices(ids: string[]) {
  return http.post<void>('/system/notice/delete', { ids })
}
