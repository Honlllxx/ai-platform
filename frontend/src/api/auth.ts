import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  remember?: boolean
}

export interface LoginResult {
  token: string
  refreshToken: string
  userInfo: {
    id: number
    username: string
    nickname: string
    avatar: string
  }
}

export function login(data: LoginParams) {
  return request.post<any, LoginResult>('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function refreshToken(refreshToken: string) {
  return request.post<any, { token: string; refreshToken: string }>('/auth/refresh', { refreshToken })
}

export function getCurrentUser() {
  return request.get('/auth/current-user')
}

export function getDashboardStats() {
  return request.get('/dashboard/stats')
}

export function getOperationTrend() {
  return request.get('/dashboard/operation-trend')
}

export function getOperationTypeDistribution() {
  return request.get('/dashboard/operation-type-distribution')
}
