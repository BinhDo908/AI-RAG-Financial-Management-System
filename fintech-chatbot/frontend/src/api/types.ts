// Kiểu dữ liệu khớp DTO của backend (com.fintech.chatbot.dto.*)

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  errorCode?: string
  details?: Record<string, string> | null
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type Role = 'USER' | 'ADMIN'

export interface User {
  id: number
  email: string
  fullName: string
  role: Role
  status: 'ACTIVE' | 'LOCKED'
  createdAt: string
  lastLoginAt?: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export type InputMode = 'TEXT' | 'VOICE' | 'IMAGE'
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'ADJUST'

export interface Citation {
  number: number
  documentId: number
  chunkId: number
  title: string
  heading?: string | null
  pageNumber?: number | null
  url?: string | null
  sourceId?: string
}

export interface TransactionDraft {
  type: 'INCOME' | 'EXPENSE'
  amount: number
  categoryId: number | null
  categoryName: string | null
  walletId: number | null
  walletName: string | null
  txnDate: string
  note: string | null
}

export type ChatPayload =
  | { type: 'TRANSACTION_DRAFTS'; drafts: TransactionDraft[] }
  | { type: 'TRANSACTIONS_SAVED'; transactionIds: number[] }
  | { type: 'PERSONAL_DATA'; result: Record<string, unknown> }
  | { type: 'FINANCIAL_SUMMARY'; summary: Record<string, unknown> }
  | { type: 'FX_RATE'; base: string; quote: string; rate: number; sourceId: string; fetchedAt: string }
  | { type: 'CLARIFY'; options: string[] }

export interface ChatMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  inputMode?: InputMode
  intent?: string
  citations?: Citation[] | null
  payload?: ChatPayload | null
  noSource?: boolean
  latencyMs?: number
  createdAt: string
}

export interface ChatResponse {
  conversationId: number
  conversationTitle: string
  userMessage: ChatMessage
  reply: ChatMessage
}

export interface Conversation {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ConversationDetail extends Conversation {
  messages: ChatMessage[]
}

export interface TransactionRequest {
  type: TransactionType
  amount: number
  walletId: number
  toWalletId?: number | null
  categoryId?: number | null
  txnDate?: string
  note?: string | null
}

export interface TransactionResult {
  transaction: { id: number; amount: number; type: TransactionType }
  walletBalance: number
  negativeBalance: boolean
}

export interface ConfirmTransactionsResponse {
  results: TransactionResult[]
  reply: ChatMessage | null
}

export interface Category {
  id: number
  name: string
  type: 'INCOME' | 'EXPENSE'
  icon?: string | null
  parentId?: number | null
  hidden: boolean
  defaultCategory: boolean
}

export interface Wallet {
  id: number
  name: string
  type: 'CASH' | 'BANK' | 'EWALLET'
  initialBalance: number
  currentBalance: number
  defaultWallet: boolean
  archived: boolean
}

export interface WalletList {
  wallets: Wallet[]
  totalBalance: number
}

export type FeedbackRating = 'UP' | 'DOWN'
export type FeedbackReason = 'WRONG_INFO' | 'NO_SOURCE' | 'IRRELEVANT' | 'WRONG_NUMBER' | 'OTHER'
