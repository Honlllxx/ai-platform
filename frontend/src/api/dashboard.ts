import request from '@/utils/request'

export interface DashboardStats {
  userCount: number
  todayOperations: number
  agentCalls: number
  activeAgents: number
  autoPassRate: number
  manualCorrectionRate: number
  costSavings: number
  avgProcessingTime: number
}

export interface DailyProcessing {
  date: string
  autoPassed: number
  manualCorrected: number
}

export interface DocTypeDistribution {
  type: string
  value: number
}

export interface OperationTrend {
  date: string
  count: number
}

/** Get overview statistics for the dashboard. */
export function getDashboardStats() {
  return request.get<any, DashboardStats>('/dashboard/stats')
}

/** Get daily processing volume trend. */
export function getDailyProcessing(params?: { days?: number }) {
  return request.get<any, DailyProcessing[]>('/dashboard/daily-processing', { params })
}

/** Get document type distribution. */
export function getDocTypeDistribution() {
  return request.get<any, DocTypeDistribution[]>('/dashboard/doc-type-distribution')
}

/** Get operation trend over time. */
export function getOperationTrend(params?: { days?: number }) {
  return request.get<any, OperationTrend[]>('/dashboard/operation-trend', { params })
}

/** Get operation type distribution (pie chart data). */
export function getOperationTypeDistribution() {
  return request.get<any, { name: string; value: number }[]>('/dashboard/operation-type-distribution')
}
