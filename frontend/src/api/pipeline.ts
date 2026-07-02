import request from '@/utils/request'

export interface PipelineItem {
  id: number
  documentId: number
  documentName: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  currentStep: string
  totalSteps: number
  completedSteps: number
  duration: number  // seconds
  startTime: string
  endTime?: string
  error?: string
}

export interface PipelineDetail extends PipelineItem {
  stateGraph: StateGraphData
  extractionResults: ExtractionField[]
  validationReport: ValidationItem[]
  reflectionData: ReflectionItem[]
}

export interface StateGraphData {
  nodes: { id: string; label: string; type: string; status: string; duration: number }[]
  edges: { from: string; to: string; label?: string }[]
}

export interface ExtractionField {
  id: number
  fieldName: string
  extractedValue: string
  confidence: number  // 0-1
  status: 'verified' | 'uncertain' | 'rejected'
  pageRef?: number
}

export interface ValidationItem {
  id: number
  rule: string
  field: string
  severity: 'error' | 'warning' | 'info'
  message: string
  suggestion?: string
}

export interface ReflectionItem {
  id: number
  fieldName: string
  beforeValue: string
  afterValue: string
  reason: string
  timestamp: string
}

export function getPipelines(params: { page: number; pageSize: number; status?: string; keyword?: string }) {
  return request.get('/pipeline', { params })
}

export function getPipeline(id: number) {
  return request.get(`/pipeline/${id}`)
}

export function getPipelineExtraction(id: number) {
  return request.get(`/pipeline/${id}/extraction`)
}

export function getPipelineValidation(id: number) {
  return request.get(`/pipeline/${id}/validation`)
}

export function getPipelineReflection(id: number) {
  return request.get(`/pipeline/${id}/reflection`)
}

// Document sub-resources
export function getDocumentLayout(id: number) {
  return request.get(`/documents/${id}/layout`)
}

export function getDocumentOcr(id: number) {
  return request.get(`/documents/${id}/ocr`)
}

export function getDocumentTable(id: number) {
  return request.get(`/documents/${id}/table`)
}
