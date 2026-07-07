import request from '@/utils/request'

export interface BaseModel {
  id: number
  name: string
  version: string
  type: 'base' | 'lora' | 'dataset'
  description: string
  size: string  // e.g. "7B", "13B"
  status: 'active' | 'inactive' | 'training'
  framework?: string
  metrics?: { accuracy: number; f1: number; inferenceTime: number }
  createTime: string
}

export interface ModelBinding {
  id: number
  baseModelId: number
  baseModelName: string
  loraAdapterId: number
  loraAdapterName: string
  datasetId: number
  datasetName: string
  bindingName: string
  status: 'active' | 'inactive'
  createTime: string
}

// ── Model assets ──

/** List model assets (fixed: removed extra `/assets` segment). */
export function getBaseModels(params: { page: number; pageSize: number; type?: string }) {
  return request.get('/models', { params })
}

/** Get a single model asset by ID. */
export function getModelDetail(id: number) {
  return request.get(`/models/${id}`)
}

/** Create a new model asset. */
export function createModelAsset(data: Partial<BaseModel>) {
  return request.post('/models', data)
}

/** Update a model asset. */
export function updateModelAsset(id: number, data: Partial<BaseModel>) {
  return request.put(`/models/${id}`, data)
}

/** Delete (soft-delete) a model asset. */
export function deleteModelAsset(id: number) {
  return request.delete(`/models/${id}`)
}

// ── Model bindings ──

/** List model bindings (fixed: added missing `/list` segment). */
export function getModelBindings(params: { page: number; pageSize: number }) {
  return request.get('/models/bindings/list', { params })
}

/** Create a new model binding. */
export function createModelBinding(data: { baseModelId: number; loraAdapterId: number; datasetId: number; bindingName: string }) {
  return request.post('/models/bindings', data)
}

/** Delete a model binding. */
export function deleteModelBinding(id: number) {
  return request.delete(`/models/bindings/${id}`)
}

/** Set a binding as active. */
export function setActiveBinding(id: number) {
  return request.post(`/models/bindings/${id}/activate`)
}
