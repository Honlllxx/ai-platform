import request from '@/utils/request'

export interface PromptVersion {
  id: number
  name: string
  version: string
  documentType: string
  stage: string  // 'layout_analysis'|'ocr'|'table_extraction'|'field_extraction'|'validation'
  content: string
  author: string
  status: 'draft' | 'active' | 'archived'
  commitMessage?: string
  gitCommitHash?: string
  createTime: string
  updateTime: string
}

export interface PromptDiff {
  versionA: string
  versionB: string
  diffLines: { type: 'added' | 'removed' | 'unchanged' | 'modified'; lineNumber: number; content: string }[]
}

export function getPromptVersions(params: { page: number; pageSize: number; documentType?: string; stage?: string }) {
  return request.get('/prompt/versions', { params })
}

export function getPromptVersion(id: number) {
  return request.get(`/prompt/versions/${id}`)
}

export function createPromptVersion(data: Partial<PromptVersion>) {
  return request.post('/prompt/versions', data)
}

export function updatePromptVersion(id: number, data: Partial<PromptVersion>) {
  return request.put(`/prompt/versions/${id}`, data)
}

export function rollbackPromptVersion(id: number) {
  return request.post(`/prompt/versions/${id}/rollback`)
}

export function getPromptDiff(idA: number, idB: number) {
  return request.get('/prompt/versions/diff', { params: { versionA: idA, versionB: idB } })
}

// Evaluation
export interface EvaluationItem {
  id: number
  taskName: string
  promptVersionId: number
  promptVersionName: string
  datasetSize: number
  accuracy: number
  precision: number
  recall: number
  f1Score: number
  status: 'pending' | 'running' | 'completed' | 'failed'
  duration: number
  createTime: string
}

export function getEvaluations(params: { page: number; pageSize: number }) {
  return request.get('/prompt/evaluation', { params })
}

export function runEvaluation(data: { promptVersionId: number; datasetName: string }) {
  return request.post('/prompt/evaluation/run', data)
}
