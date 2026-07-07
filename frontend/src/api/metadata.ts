import request from '@/utils/request'

export interface MetadataField {
  id: number
  name: string
  code: string
  type: 'string' | 'number' | 'date' | 'enum' | 'boolean'
  length?: number
  enumValues?: string  // comma-separated
  validationRule?: string
  groupCode: string  // document type: 'contract'|'invoice'|'bill_of_lading'
  groupName: string
  required: boolean
  description?: string
  sort: number
  status: number
  createTime: string
}

export function getMetadataFields(params: { page: number; pageSize: number; groupCode?: string; keyword?: string }) {
  return request.get('/metadata/fields', { params })
}

export function getMetadataField(id: number) {
  return request.get(`/metadata/fields/${id}`)
}

export function createMetadataField(data: Partial<MetadataField>) {
  return request.post('/metadata/fields', data)
}

export function updateMetadataField(id: number, data: Partial<MetadataField>) {
  return request.put(`/metadata/fields/${id}`, data)
}

export function deleteMetadataField(id: number) {
  return request.delete(`/metadata/fields/${id}`)
}

export function getMetadataGroups() {
  return request.get('/metadata/groups')
}

export function exportData(params: { format: 'json' | 'xml' | 'excel'; documentIds?: number[]; startDate?: string; endDate?: string }) {
  return request.post('/metadata/export', params)
}

export function getExportHistory(params: { page: number; pageSize: number }) {
  return request.get('/metadata/export/history', { params })
}

export function getWebhookConfig() {
  return request.get('/metadata/export/webhook')
}

export function updateWebhookConfig(data: { url: string; secret: string; enabled: boolean; events: string[] }) {
  return request.put('/metadata/export/webhook', data)
}

/** Get metadata fields filtered by group code. */
export function getMetadataFieldsByGroup(groupCode: string) {
  return request.get(`/metadata/fields/by-group/${groupCode}`)
}
