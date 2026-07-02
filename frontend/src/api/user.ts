import request from '@/utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: 0 | 1
  roleIds: number[]
  roleNames: string[]
  createTime: string
}

export interface UserQuery {
  page: number
  pageSize: number
  username?: string
  email?: string
  status?: number
}

export interface CreateUserParams {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  roleIds?: number[]
}

export interface UpdateUserParams {
  id: number
  nickname?: string
  email?: string
  phone?: string
  roleIds?: number[]
  status?: 0 | 1
}

export function getUsers(params: UserQuery) {
  return request.get('/users', { params })
}

export function getUserById(id: number) {
  return request.get(`/users/${id}`)
}

export function createUser(data: CreateUserParams) {
  return request.post('/users', data)
}

export function updateUser(data: UpdateUserParams) {
  return request.put(`/users/${data.id}`, data)
}

export function deleteUser(id: number) {
  return request.delete(`/users/${id}`)
}

export function updateUserStatus(id: number, status: 0 | 1) {
  return request.patch(`/users/${id}/status`, { status })
}

export function assignRoles(userId: number, roleIds: number[]) {
  return request.post(`/users/${userId}/roles`, { roleIds })
}
