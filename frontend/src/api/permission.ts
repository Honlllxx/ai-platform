import request from '@/utils/request'

export interface PermissionInfo {
  id: number
  name: string
  code: string
  type: 'menu' | 'button' | 'api'
  parentId: number | null
  path?: string
  icon?: string
  sort: number
  status: 0 | 1
  children?: PermissionInfo[]
}

export function getPermissions() {
  return request.get('/permissions')
}

export function getPermissionTree() {
  return request.get('/permissions/tree')
}

/** Get flat permission list (no tree nesting). */
export function getFlatPermissions() {
  return request.get('/permissions/flat')
}

/** Get a single permission by ID. */
export function getPermissionById(id: number) {
  return request.get(`/permissions/${id}`)
}

export function createPermission(data: Partial<PermissionInfo>) {
  return request.post('/permissions', data)
}

export function updatePermission(data: Partial<PermissionInfo>) {
  return request.put(`/permissions/${data.id}`, data)
}

export function deletePermission(id: number) {
  return request.delete(`/permissions/${id}`)
}
