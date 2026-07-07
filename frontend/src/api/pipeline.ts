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

// ── Pipeline ──

export function getPipelines(params: { page: number; pageSize: number; status?: string; keyword?: string }) {
  return request.get('/pipeline', { params })
}

/** Get pipeline detail — includes extraction/validation/reflection in one response. */
export function getPipeline(id: number) {
  return request.get(`/pipeline/${id}`)
}

/** Trigger a full pipeline run for a document. */
export function runPipeline(documentId: number, metadataFields?: Array<{ code: string; name: string; type: string }>) {
  return request.post('/pipeline/run', metadataFields || [], { params: { documentId } })
}

// ── Document sub-resources (moved to document.ts as well, kept here for backward compat) ──

export { getDocumentLayout, getDocumentOcr, getDocumentTable } from './document'
