// ── Notification / Communication types ──

export interface NotificationItem {
  id: number
  title: string
  message: string
  type: string
  relatedUrl: string | null
  isRead: boolean
  createdAt: string
}

export interface NotificationResponse {
  notifications: NotificationItem[]
  unreadCount: number
}

export interface NotifyRequest {
  recipientType: string
  subject: string
  body: string
}

export interface NotifyResult {
  sent: number
  total: number
  recipientType: string
}

export interface MessageLogEntry {
  id: number
  recipientType: string
  subject: string
  body: string
  recipientCount: number
  createdAt: string
}
