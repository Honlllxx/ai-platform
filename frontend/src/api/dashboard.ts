import request from '@/utils/request'

export interface DashboardStats {
  totalDocuments: number
  processingDocuments: number
  completedDocuments: number
  failedDocuments: number
  totalPipelines: number
  avgProcessingTime: number
  successRate: number
}

export interface DailyProcessing {
  date: string
  count: number
  completed: number
  failed: number
}

export interface DocTypeDistribution {
  type: string
  count: number
  percentage: number
}

export interface OperationTrend {
  date: string
  uploads: number
  pipelines: number
  extractions: number
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
