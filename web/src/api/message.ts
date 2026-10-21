import { http } from './request'
import type { MessageItem, MessageQuery, PageResult } from '@/types/api'

export function pageMessages(query: MessageQuery) {
  return http.get<PageResult<MessageItem>>('/message/list', { ...query })
}

export function getMessage(id: string) {
  return http.get<MessageItem>('/message/info', { id })
}

export function getUnreadCount() {
  return http.get<number>('/message/unread-count', undefined, true)
}

export function markMessagesRead(ids: string[]) {
  return http.post<void>('/message/read', { ids })
}

export function markAllMessagesRead() {
  return http.post<void>('/message/read-all')
}
