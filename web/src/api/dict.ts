import { http } from './request'
import type {
  DictDataQuery,
  DictDataRequest,
  DictTypeQuery,
  DictTypeSaveRequest,
  OptionItem,
  PageResult,
  SysDictData,
  SysDictType,
} from '@/types/api'

/* ------------------------------ 字典类型 ------------------------------ */
export function pageDictTypes(query: DictTypeQuery) {
  return http.get<PageResult<SysDictType>>('/system/dict/type/list', { ...query })
}

export function getDictTypeOptions() {
  return http.get<OptionItem[]>('/system/dict/type/options')
}

/** 一体化保存：类型 + 其全部字典项，一个事务内增/改/删 */
export function saveDictType(data: DictTypeSaveRequest) {
  return http.post<string>('/system/dict/type/save', data)
}

export function deleteDictType(id: string) {
  return http.post<void>('/system/dict/type/delete', { ids: [id] })
}

/* ------------------------------ 字典数据 ------------------------------ */
export function pageDictData(query: DictDataQuery) {
  return http.get<PageResult<SysDictData>>('/system/dict/data/list', { ...query })
}

/** 按类型取启用字典项，供下拉/标签渲染 */
export function getDictByType(dictType: string) {
  return http.get<SysDictData[]>(`/system/dict/data/type/${dictType}`)
}

export function createDictData(data: DictDataRequest) {
  return http.post<string>('/system/dict/data', data)
}

export function updateDictData(id: string, data: DictDataRequest) {
  return http.post<void>('/system/dict/data/update', { id, ...data })
}

export function deleteDictData(id: string) {
  return http.post<void>('/system/dict/data/delete', { ids: [id] })
}
