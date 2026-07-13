import request from '@/utils/request'

export interface ReviewItem {
  id: number
  documentId: number
  documentName: string
  pipelineId?: number
  documentType: string
  status: 'pending' | 'reviewing' | 'approved' | 'rejected' | 'partial'
  totalFields: number
  reviewedFields: number
  approvedFields: number
  rejectedFields: number
  reviewerId?: number
  reviewer?: string
  startedAt?: string
  reviewTime?: string
  reviewedAt?: string
  reviewComment?: string
  createTime: string
  updateTime?: string
}

export interface ReviewField {
  id: number
  fieldName: string
  fieldCode: string
  extractedValue: string
  confidence: number
  imageRegion?: string
  originalImageRegion?: { x: number; y: number; width: number; height: number }
  status: 'pending' | 'approved' | 'rejected' | 'corrected'
  correctedValue?: string
  comment?: string
}

export interface ReviewStats {
  pendingCount: number
  reviewingCount: number
  todayReviewed: number
  approvalRate: number
  totalReviews: number
}

export interface CreateReviewParams {
  documentId: number
  documentType: string
  pipelineId?: number
  reviewerId?: number
  fields?: Array<{
    fieldName: string
    fieldCode: string
    extractedValue: string
    confidence: number
    imageRegion?: string
  }>
}

// ==================== Stats ====================

export function getReviewStats() {
  return request.get<ReviewStats>('/review/stats')
}

export function getReviewStatistics(params?: {
  startDate?: string
  endDate?: string
  documentType?: string
  reviewerId?: number
}) {
  return request.get('/review/statistics', { params })
}

// ==================== CRUD ====================

export function getReviews(params: {
  page: number
  pageSize: number
  status?: string
  keyword?: string
  documentType?: string
}) {
  return request.get('/review', { params })
}

export function createReview(data: CreateReviewParams) {
  return request.post('/review', data)
}

export function getReviewDetail(id: number) {
  return request.get(`/review/${id}`)
}

export function updateReviewStatus(id: number, status: string) {
  return request.put(`/review/${id}/status`, { status })
}

export function deleteReview(id: number) {
  return request.delete(`/review/${id}`)
}

// ==================== Assignment ====================

export function assignReviewer(id: number, reviewerId: number) {
  return request.put(`/review/${id}/assign`, { reviewerId })
}

// ==================== Workflow ====================

export function startReview(id: number) {
  return request.post(`/review/${id}/start`)
}

export function saveReviewComment(id: number, comment: string) {
  return request.put(`/review/${id}/comment`, { comment })
}

// ==================== Fields ====================

export function getReviewFields(reviewId: number) {
  return request.get(`/review/${reviewId}/fields`)
}

export function approveField(reviewId: number, fieldId: number) {
  return request.post(`/review/${reviewId}/fields/${fieldId}/approve`)
}

export function rejectField(reviewId: number, fieldId: number, comment?: string) {
  return request.post(`/review/${reviewId}/fields/${fieldId}/reject`, { comment })
}

export function correctField(reviewId: number, fieldId: number, correctedValue: string) {
  return request.post(`/review/${reviewId}/fields/${fieldId}/correct`, { correctedValue })
}

export function batchApprove(reviewId: number, fieldIds: number[]) {
  return request.post(`/review/${reviewId}/batch-approve`, { fieldIds })
}

export function batchReject(reviewId: number, fieldIds: number[]) {
  return request.post(`/review/${reviewId}/batch-reject`, { fieldIds })
}

export function batchCorrect(reviewId: number, fieldIds: number[], correctedValue?: string) {
  return request.post(`/review/${reviewId}/batch-correct`, { fieldIds, correctedValue })
}

// ==================== History ====================

export function getReviewHistory(reviewId: number, params?: {
  page?: number
  pageSize?: number
  action?: string
}) {
  return request.get(`/review/${reviewId}/history`, { params })
}