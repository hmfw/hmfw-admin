import { http } from './request'
import type { DashboardSummary } from '@/types/api'

export function getDashboardSummary() {
  return http.get<DashboardSummary>('/dashboard/summary')
}
