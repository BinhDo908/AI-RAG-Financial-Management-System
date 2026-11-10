import { api, ApiError, qs, tokenStore } from './client'
import type { Category, PageResponse, TransactionRequest, TransactionType, WalletList, Wallet } from './types'

// UC-A01, A02, A04, A08, A09, A10 — khớp DTO ở backend

export type TransactionSource = 'MANUAL' | 'CHAT' | 'VOICE' | 'RECEIPT' | 'RECURRING'
export type WalletType = 'CASH' | 'BANK' | 'EWALLET'

export interface Transaction {
  id: number
  type: TransactionType
  amount: number
  txnDate: string
  note: string | null
  source: TransactionSource
  walletId: number
  walletName: string
  toWalletId: number | null
  toWalletName: string | null
  categoryId: number | null
  categoryName: string | null
  debtId: number | null
  goalId: number | null
  createdAt: string
}

export interface TransactionResult {
  transaction: Transaction
  walletBalance: number
  negativeBalance: boolean
}

export interface TransactionSearch {
  page: PageResponse<Transaction>
  totalIncome: number
  totalExpense: number
  net: number
}

export interface TransactionFilter {
  from?: string
  to?: string
  walletId?: number
  categoryId?: number
  type?: TransactionType
  keyword?: string
  source?: TransactionSource
}

export type BudgetStatus = 'OK' | 'WARNING' | 'OVER'

export interface Budget {
  id: number
  categoryId: number
  categoryName: string
  month: string
  limitAmount: number
  warnThreshold: number
  spent: number
  remaining: number
  percentUsed: number
  status: BudgetStatus
}

export interface CategoryShare {
  categoryId: number
  categoryName: string
  amount: number
  percent: number
}

export interface ReportOverview {
  from: string
  to: string
  totalIncome: number
  totalExpense: number
  net: number
  expenseByCategory: CategoryShare[]
  topExpenseCategories: CategoryShare[]
  monthlyTrend: { month: string; income: number; expense: number }[]
  balanceHistory: { date: string; totalBalance: number }[]
  netWorth: { walletBalance: number; lending: number; borrowing: number; netWorth: number }
}

export const financeApi = {
  // A09
  overview: (from?: string, to?: string) => api.get<ReportOverview>('/api/reports/overview' + qs({ from, to })),

  // A02, A08
  transactions: (f: TransactionFilter, page = 0, size = 30) =>
    api.get<TransactionSearch>('/api/transactions' + qs({ ...f, page, size })),
  createTransaction: (body: TransactionRequest) => api.post<TransactionResult>('/api/transactions', { ...body }),
  updateTransaction: (id: number, body: TransactionRequest) => api.put<TransactionResult>(`/api/transactions/${id}`, { ...body }),
  deleteTransaction: (id: number) => api.delete(`/api/transactions/${id}`),

  // A01
  wallets: (includeArchived = false) => api.get<WalletList>('/api/wallets' + qs({ includeArchived })),
  createWallet: (body: { name: string; type: WalletType; initialBalance: number; defaultWallet: boolean }) =>
    api.post<Wallet>('/api/wallets', body),
  updateWallet: (id: number, body: { name: string; type: WalletType; defaultWallet: boolean }) => api.put<Wallet>(`/api/wallets/${id}`, body),
  adjustBalance: (id: number, newBalance: number, note?: string) =>
    api.post<TransactionResult>(`/api/wallets/${id}/adjust-balance`, { newBalance, note }),
  archiveWallet: (id: number, archived: boolean) => api.post<Wallet>(`/api/wallets/${id}/${archived ? 'archive' : 'unarchive'}`),
  deleteWallet: (id: number) => api.delete(`/api/wallets/${id}`),

  // A03 (chỉ đọc ở đây)
  categories: () => api.get<Category[]>('/api/categories'),

  // A04
  budgets: (month: string) => api.get<Budget[]>('/api/budgets' + qs({ month })),
  createBudget: (body: { categoryId: number; month: string; limitAmount: number; warnThreshold?: number }) =>
    api.post<Budget>('/api/budgets', body),
  updateBudget: (id: number, body: { limitAmount: number; warnThreshold?: number }) => api.put<Budget>(`/api/budgets/${id}`, body),
  deleteBudget: (id: number) => api.delete(`/api/budgets/${id}`),
  copyPreviousBudgets: (month: string) => api.post<Budget[]>('/api/budgets/copy-previous' + qs({ month })),

  /** A10: tải file (cần header Authorization nên không dùng thẻ <a> trực tiếp). */
  exportTransactions: async (from: string, to: string, format: 'CSV' | 'XLSX') => {
    const res = await fetch('/api/transactions/export' + qs({ from, to, format }), {
      headers: { Authorization: `Bearer ${tokenStore.get() ?? ''}` },
    })
    if (!res.ok) {
      const body = await res.json().catch(() => null)
      throw new ApiError(res.status, body?.errorCode ?? 'HTTP_' + res.status, body?.message ?? 'Không xuất được file')
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `giao-dich_${from}_${to}.${format === 'CSV' ? 'csv' : 'xlsx'}`
    a.click()
    URL.revokeObjectURL(url)
  },
}
