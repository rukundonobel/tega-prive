import { api } from './api'
import type { ApiResponse } from '@/types'

export interface NotificationItem {
  id: string
  type: string
  title: string
  body: string
  unread: boolean
  createdAt: string
}

export const notificationsService = {
  list: () =>
    api.get<ApiResponse<NotificationItem[]>>('/notifications').then((r) => r.data.data),
  unreadCount: () =>
    api.get<ApiResponse<number>>('/notifications/unread-count').then((r) => r.data.data),
  markAllRead: () =>
    api.post<ApiResponse<void>>('/notifications/mark-read').then((r) => r.data),
}
