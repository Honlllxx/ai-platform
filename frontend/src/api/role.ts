import request from '@/utils/request'

export interface RoleInfo {
  id: number
  name: string
  code: string
  description: string
  status: 0 | 1
  permissionIds: number[]
  userCount: number
  createTime: string
}

export interface RoleQuery {
  page: number
  pageSize: number
  name?: string
  code?: string
}

export interface CreateRoleParams {
  name: string
  code: string
  description?: string
  permissionIds?: number[]
}

export interface UpdateRoleParams {
  id: number
  name?: string
  description?: string
  permissionIds?: number[]
}

export function getRoles(params: RoleQuery) {
  return request.get('/roles', { params })
}

export function getAllRoles() {
  return request.get('/roles/all')
}

export function getRoleById(id: number) {
  return request.get(`/roles/${id}`)
}

export function createRole(data: CreateRoleParams) {
  return request.post('/roles', data)
}

export function updateRole(data: UpdateRoleParams) {
  return request.put(`/roles/${data.id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}

export function assignPermissions(roleId: number, permissionIds: number[]) {
  return request.post(`/roles/${roleId}/permissions`, { permissionIds })
}
