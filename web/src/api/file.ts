import { http } from './request'
import type { FileQuery, PageResult, SysFile } from '@/types/api'

/** 上传文件（返回文件信息） */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<SysFile>('/system/file/upload', formData)
}

/** 分页查询文件列表 */
export function listFiles(query: FileQuery) {
  return http.get<PageResult<SysFile>>('/system/file/list', { ...query })
}

/** 批量删除文件 */
export function deleteFiles(ids: string[]) {
  return http.post<void>('/system/file/delete', { ids })
}

/** 下载文件（返回 Blob） */
export function downloadFile(id: string) {
  return http.getBlob('/system/file/download', { id })
}
