import { api, qs } from './client'
import type { FeedbackReason, PageResponse, User } from './types'

// UC-C02, C03, C05 — khớp DTO ở backend (dto.response.ChatQualityResponses, FeedbackResponses)

export interface EvalRunView {
  id: number
  datasetName: string
  totalQuestions: number
  hitRate: number
  citationRate: number
  accuracy: number
  createdAt: string
}

/** Các tỷ lệ (voiceRatio, thumbsUpRatio, noSourceRate, hitRate…) nằm trong khoảng 0–1. */
export interface QualityDashboard {
  from: string
  to: string
  activeUsers: number
  conversations: number
  userMessages: number
  intentDistribution: Record<string, number>
  voiceRatio: number
  thumbsUp: number
  thumbsDown: number
  thumbsUpRatio: number
  knowledgeAnswers: number
  noSourceAnswers: number
  noSourceRate: number
  unansweredQuestions: string[]
  averageLatencyMs: number | null
  latestEvaluation: EvalRunView | null
}

export interface ReportSummary {
  id: number
  messageId: number
  intent: string | null
  reason: FeedbackReason | null
  comment: string | null
  reviewStatus: 'OPEN' | 'RESOLVED' | 'REJECTED'
  createdAt: string
}

export const adminApi = {
  /** Thời điểm dạng ISO local (yyyy-MM-ddTHH:mm:ss) như backend yêu cầu. */
  dashboard: (from: string, to: string) => api.get<QualityDashboard>('/api/admin/chat-quality' + qs({ from, to })),
  openReports: (size = 5) => api.get<PageResponse<ReportSummary>>('/api/admin/feedback' + qs({ status: 'OPEN', size })),
  users: (params: { status?: 'ACTIVE' | 'LOCKED'; role?: 'USER' | 'ADMIN'; size?: number } = {}) =>
    api.get<PageResponse<User>>('/api/admin/users' + qs({ size: 1, ...params })),
}
