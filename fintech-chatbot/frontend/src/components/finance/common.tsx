import { useEffect, useState, type ReactNode } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { currentMonth, formatMonth, shiftMonth } from '../../lib/format'
import AccountMenu from '../AccountMenu'

/** Khung trang tài chính: cuộn dọc, nội dung tối đa ~1100px. */
export function FinancePage({ children }: { children: ReactNode }) {
  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto grid max-w-[1120px] grid-cols-[minmax(0,1fr)] gap-4 px-4 py-6 sm:px-6 sm:py-8">{children}</div>
    </div>
  )
}

/**
 * Tiêu đề trang + thao tác. Điện thoại: hàng 1 là tiêu đề và nút tài khoản, hàng 2 là thao tác (rộng hết hàng);
 * máy tính: một hàng, tài khoản nằm ở thanh dọc bên trái.
 */
export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="mb-1 flex flex-wrap items-center gap-3">
      <h1 className="mr-auto text-xl font-semibold tracking-[-0.02em] text-ink">{title}</h1>
      {children && <div className="order-last flex w-full flex-wrap items-center gap-2 sm:order-none sm:w-auto">{children}</div>}
      <div className="md:hidden">
        <AccountMenu placement="top" />
      </div>
    </div>
  )
}

/** Chọn tháng ‹ Tháng 10/2026 ›; không cho đi quá tháng hiện tại. */
export function MonthStepper({ value, onChange, allowFuture = false }: { value: string; onChange: (m: string) => void; allowFuture?: boolean }) {
  const atLatest = !allowFuture && value >= currentMonth()
  return (
    <div className="inline-flex h-9 items-center rounded-lg border border-stone-200 bg-white">
      <button onClick={() => onChange(shiftMonth(value, -1))} className="btn btn-ghost btn-icon h-full rounded-r-none" aria-label="Tháng trước">
        <ChevronLeft size={16} />
      </button>
      <span className="tabular min-w-28 px-1 text-center text-sm font-medium text-ink">{formatMonth(value)}</span>
      <button
        onClick={() => onChange(shiftMonth(value, 1))}
        disabled={atLatest}
        className="btn btn-ghost btn-icon h-full rounded-l-none"
        aria-label="Tháng sau"
      >
        <ChevronRight size={16} />
      </button>
    </div>
  )
}

/** Ô số liệu: nhãn nhỏ + giá trị; màu chỉ dùng cho dấu hiệu tăng/giảm, không cho trang trí. */
export function StatTile({
  label,
  value,
  tone = 'neutral',
  className = '',
}: {
  label: string
  value: string
  tone?: 'neutral' | 'positive' | 'negative'
  className?: string
}) {
  const color = tone === 'positive' ? 'text-accent-700' : tone === 'negative' ? 'text-negative' : 'text-ink'
  return (
    <div className={`min-w-0 rounded-xl border border-stone-200 bg-white px-3 py-3 sm:px-4 sm:py-3.5 ${className}`}>
      <div className="text-xs text-stone-500">{label}</div>
      <div className={`tabular mt-1 truncate text-[15px] font-semibold sm:text-xl tracking-[-0.01em] ${color}`}>{value}</div>
    </div>
  )
}

/** Ô nhập số tiền: hiển thị phân cách hàng nghìn khi gõ, trả về số nguyên VND. */
export function MoneyInput({
  value,
  onChange,
  id,
  large = false,
  placeholder = '0',
  autoFocus,
}: {
  value: number | null
  onChange: (v: number | null) => void
  id?: string
  large?: boolean
  placeholder?: string
  autoFocus?: boolean
}) {
  const fmt = (v: number | null) => (v === null ? '' : v.toLocaleString('vi-VN'))
  const [text, setText] = useState(fmt(value))
  useEffect(() => {
    setText((prev) => (parseDigits(prev) === value ? prev : fmt(value)))
  }, [value])
  return (
    <div className="relative">
      <input
        id={id}
        inputMode="numeric"
        autoComplete="off"
        autoFocus={autoFocus}
        className={`input pr-8 ${large ? 'input-amount' : 'tabular'}`}
        placeholder={placeholder}
        value={text}
        onChange={(e) => {
          const v = parseDigits(e.target.value)
          setText(fmt(v))
          onChange(v)
        }}
      />
      <span className={`pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-stone-400 ${large ? 'text-lg' : 'text-sm'}`}>đ</span>
    </div>
  )
}

function parseDigits(s: string): number | null {
  const digits = s.replace(/\D/g, '')
  return digits ? Number(digits) : null
}

export function EmptyState({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-stone-300 px-6 py-12 text-center">
      <p className="text-sm font-medium text-ink">{title}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorNote({ message }: { message: string }) {
  return <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-negative">{message}</p>
}

/** Khối chờ tải: giữ đúng bố cục để trang không nhảy. */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-stone-100 ${className}`} />
}
