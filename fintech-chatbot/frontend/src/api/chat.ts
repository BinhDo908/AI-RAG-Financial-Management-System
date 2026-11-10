import { api, ApiError, notifyUnauthorized, qs, tokenStore } from './client'
import type {
  Category,
  ChatResponse,
  ConfirmTransactionsResponse,
  Conversation,
  ConversationDetail,
  FeedbackRating,
  FeedbackReason,
  InputMode,
  PageResponse,
  TransactionRequest,
  WalletList,
} from './types'

/** Bước xử lý server đang làm (sự kiện "stage" của stream). */
export type ReplyStage = 'THINKING' | 'SEARCHING' | 'WRITING'

interface StreamHandlers {
  onStage?: (stage: ReplyStage) => void
  /** Đoạn chữ vừa sinh; ghép lại thành câu trả lời tạm thời. */
  onDelta?: (text: string) => void
  signal?: AbortSignal
}

/**
 * Gửi tin nhắn và đọc câu trả lời dạng Server-Sent Events: stage → delta… → done (ChatResponse đầy đủ, kèm trích dẫn
 * và disclaimer) hoặc error. Dùng fetch vì EventSource không gửi được POST kèm header Authorization.
 */
async function sendStream(
  message: string,
  conversationId: number | null,
  inputMode: InputMode,
  handlers: StreamHandlers = {},
): Promise<ChatResponse> {
  const token = tokenStore.get()
  let res: Response
  try {
    res = await fetch('/api/chat/messages/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message, conversationId, inputMode }),
      signal: handlers.signal,
    })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, 'NETWORK_ERROR', 'Không kết nối được máy chủ. Kiểm tra mạng hoặc backend đã chạy chưa.')
  }
  if (!res.ok || !res.body) {
    if (res.status === 401 && token) notifyUnauthorized()
    const body = await res.json().catch(() => null)
    throw new ApiError(res.status, body?.errorCode ?? 'HTTP_' + res.status, body?.message ?? `Lỗi máy chủ (HTTP ${res.status})`, body?.details ?? null)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
    // Mỗi sự kiện SSE kết thúc bằng một dòng trống
    let end: number
    while ((end = buffer.indexOf('\n\n')) >= 0) {
      const raw = buffer.slice(0, end)
      buffer = buffer.slice(end + 2)
      let event = 'message'
      const data: string[] = []
      for (const line of raw.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
      }
      if (!data.length) continue
      const payload = JSON.parse(data.join('\n'))
      if (event === 'stage') handlers.onStage?.(payload.stage)
      else if (event === 'delta') handlers.onDelta?.(payload.text)
      else if (event === 'done') return payload as ChatResponse
      else if (event === 'error') throw new ApiError(200, payload.errorCode, payload.message)
    }
  }
  throw new ApiError(0, 'STREAM_CLOSED', 'Kết nối bị ngắt trước khi có câu trả lời, vui lòng thử lại.')
}

export const chatApi = {
  /** UC-A11–A13, B01–B03, B07 qua Intent Router. */
  send: (message: string, conversationId: number | null, inputMode: InputMode) =>
    api.post<ChatResponse>('/api/chat/messages', { message, conversationId, inputMode }),

  /** Như send nhưng câu trả lời hiện dần (SSE). */
  sendStream,

  /** UC-A14: ảnh hóa đơn → thẻ giao dịch nháp. */
  receipt: (image: Blob, conversationId: number | null) => {
    const form = new FormData()
    form.append('image', image, 'receipt.jpg')
    return api.post<ChatResponse>('/api/chat/receipt' + qs({ conversationId }), form)
  },

  /** UC-A11 bước 6: lưu thẻ giao dịch đã xác nhận. */
  confirmTransactions: (conversationId: number | null, inputMode: InputMode, transactions: TransactionRequest[]) =>
    api.post<ConfirmTransactionsResponse>('/api/chat/transactions', { conversationId, inputMode, transactions }),

  /** UC-B09. */
  feedback: (messageId: number, rating: FeedbackRating, reason?: FeedbackReason, comment?: string) =>
    api.post(`/api/chat/messages/${messageId}/feedback`, { rating, reason, comment }),

  conversations: (keyword?: string) =>
    api.get<PageResponse<Conversation>>('/api/conversations' + qs({ keyword, size: 50 })),
  conversation: (id: number) => api.get<ConversationDetail>(`/api/conversations/${id}`),
  renameConversation: (id: number, title: string) => api.patch<Conversation>(`/api/conversations/${id}`, { title }),
  deleteConversation: (id: number) => api.delete(`/api/conversations/${id}`),

  categories: () => api.get<Category[]>('/api/categories'),
  wallets: () => api.get<WalletList>('/api/wallets'),
}
