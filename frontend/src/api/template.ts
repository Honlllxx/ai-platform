import request from '@/utils/request'

export interface TemplateItem {
  id: number
  name: string
  documentType: string
  description?: string
  /** Backend stores regions as JSON string; frontend parses to array on read */
  regions: string | TemplateRegion[]
  status: number
  version: number
  createTime: string
  updateTime: string
}

export interface TemplateRegion {
  id: string
  x: number
  y: number
  width: number
  height: number
  fieldCode: string
  fieldName: string
  type: 'text' | 'table' | 'key_value'
}

export function getTemplates(params: { page: number; pageSize: number; documentType?: string; keyword?: string }) {
  return request.get('/templates', { params })
}

export function getTemplate(id: number) {
  return request.get(`/templates/${id}`)
}

export function createTemplate(data: Partial<TemplateItem>) {
  return request.post('/templates', data)
}

export function updateTemplate(id: number, data: Partial<TemplateItem>) {
  return request.put(`/templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/templates/${id}`)
}
