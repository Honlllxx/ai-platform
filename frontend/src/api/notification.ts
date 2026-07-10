import request from '@/utils/request'

export interface NotificationItem {
  id: number
  reviewId: number
  documentName: string
  documentType: string
  action: string
  actionLabel: string
  detail: string
  timestamp: string
  createTime: string
  isRead: boolean
}

export interface UnreadCount {
  count: number
  newAssignments: number
  inReview: number
}

export function getNotifications(limit?: number) {
  return request.get<NotificationItem[]>('/notifications', { params: { limit } })
}

export function getUnreadCount() {
  return request.get<UnreadCount>('/notifications/unread-count')
}