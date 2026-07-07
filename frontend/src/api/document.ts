import request from '@/utils/request'

// Types
export interface DocumentItem {
  id: number
  fileName: string
  fileType: string // 'pdf' | 'jpg' | 'png' | 'word'
  fileSize: number // bytes
  uploadTime: string
  status: 'pending' | 'preprocessing' | 'completed' | 'failed'
  pageCount?: number
  uploadUser?: string
}

export interface DocumentQuery {
  page: number
  pageSize: number
  fileType?: string
  status?: string
  startDate?: string
  endDate?: string
  keyword?: string
}

// List documents with pagination and filters
export function getDocuments(params: DocumentQuery) {
  return request.get('/documents', { params })
}

// Upload document(s) — supports real progress via onUploadProgress callback
export function uploadDocument(
  formData: FormData,
  onUploadProgress?: (progressEvent: ProgressEvent) => void
) {
  return request.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    ...(onUploadProgress ? { onUploadProgress } : {}),
  })
}

// Get document detail
export function getDocument(id: number) {
  return request.get(`/documents/${id}`)
}

// Delete document
export function deleteDocument(id: number) {
  return request.delete(`/documents/${id}`)
}

// Trigger preprocessing
export function preprocessDocument(id: number) {
  return request.post(`/documents/${id}/preprocess`)
}

// ── Document sub-resources ──

/** Get document layout analysis result. */
export function getDocumentLayout(id: number) {
  return request.get(`/documents/${id}/layout`)
}

/** Get document OCR result. */
export function getDocumentOcr(id: number) {
  return request.get(`/documents/${id}/ocr`)
}

/** Get document table extraction result. */
export function getDocumentTable(id: number) {
  return request.get(`/documents/${id}/table`)
}
