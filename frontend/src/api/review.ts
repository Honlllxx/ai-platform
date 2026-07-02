import request from '@/utils/request'

export interface ReviewItem {
  id: number
  documentId: number
  documentName: string
  documentType: string
  status: 'pending' | 'approved' | 'rejected' | 'partial'
  totalFields: number
  reviewedFields: number
  approvedFields: number
  rejectedFields: number
  reviewer?: string
  reviewTime?: string
  createTime: string
}

export interface ReviewField {
  id: number
  fieldName: string
  fieldCode: string
  extractedValue: string
  confidence: number
  originalImageRegion?: { x: number; y: number; width: number; height: number }
  status: 'pending' | 'approved' | 'rejected' | 'corrected'
  correctedValue?: string
  comment?: string
}

export function getReviews(params: { page: number; pageSize: number; status?: string; keyword?: string }) {
  return request.get('/review', { params })
}

export function getReviewDetail(id: number) {
  return request.get(`/review/${id}`)
}

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

export function getReviewHistory(reviewId: number) {
  return request.get(`/review/${reviewId}/history`)
}
