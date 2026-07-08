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
    email?: string
    roles: string[]
    permissions: string[]
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
