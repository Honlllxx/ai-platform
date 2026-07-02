import request from '@/utils/request'

export interface OperationLog {
  id: number
  userId: number
  username: string
  operationType: string
  module: string
  description: string
  requestParams: string
  responseResult: string
  ip: string
  duration: number
  status: 'success' | 'fail'
  createTime: string
}

export interface OperationLogQuery {
  page: number
  pageSize: number
  startTime?: string
  endTime?: string
  username?: string
  operationType?: string
  module?: string
  status?: string
}

export function getOperationLogs(params: OperationLogQuery) {
  return request.get('/operation-logs', { params })
}

export function getOperationLogById(id: number) {
  return request.get(`/operation-logs/${id}`)
}

export function getOperationTypes() {
  return request.get('/operation-logs/types')
}

export function exportOperationLogs(params: OperationLogQuery) {
  return request.get('/operation-logs/export', {
    params,
    responseType: 'blob'
  })
}
