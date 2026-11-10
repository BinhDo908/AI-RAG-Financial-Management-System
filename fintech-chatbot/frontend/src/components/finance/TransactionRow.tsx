import { ArrowLeftRight, Camera, Lock, MessageSquare, Mic, Repeat, SlidersHorizontal } from 'lucide-react'
import type { Transaction } from '../../api/finance'
import { formatDate, formatMoney } from '../../lib/format'

const SOURCE_BADGE = {
  CHAT: { Icon: MessageSquare, label: 'Từ trò chuyện' },
  VOICE: { Icon: Mic, label: 'Nhập bằng giọng nói' },
  RECEIPT: { Icon: Camera, label: 'Từ ảnh hóa đơn' },
  RECURRING: { Icon: Repeat, label: 'Giao dịch định kỳ' },
} as const

/** Giao dịch điều chỉnh / gắn với khoản nợ, mục tiêu thì không sửa ở đây. */
export function isEditable(t: Transaction) {
  return t.type !== 'ADJUST' && t.debtId === null && t.goalId === null
}

function title(t: Transaction) {
  if (t.type === 'TRANSFER') return `${t.walletName} → ${t.toWalletName ?? '…'}`
  if (t.type === 'ADJUST') return 'Điều chỉnh số dư'
  return t.categoryName ?? 'Chưa phân loại'
}

/** Thu: xanh "+"; chi: mực "−"; chuyển ví: xám, không dấu (không làm đổi tổng tài sản). */
function Amount({ t }: { t: Transaction }) {
  if (t.type === 'INCOME') return <span className="text-accent-700">+{formatMoney(t.amount)}</span>
  if (t.type === 'EXPENSE') return <span className="text-ink">−{formatMoney(t.amount)}</span>
  return <span className="text-stone-500">{formatMoney(t.amount)}</span>
}

export function TransactionRow({ transaction: t, onClick, showDate = false }: { transaction: Transaction; onClick?: () => void; showDate?: boolean }) {
  const badge = t.source !== 'MANUAL' ? SOURCE_BADGE[t.source] : null
  const editable = isEditable(t)
  const meta = [t.type === 'TRANSFER' || t.type === 'ADJUST' ? null : t.walletName, showDate ? formatDate(t.txnDate) : null].filter(Boolean).join(' · ')

  const content = (
    <>
      <span className="grid size-8 shrink-0 place-items-center rounded-full bg-stone-100 text-xs font-semibold text-stone-600" aria-hidden>
        {t.type === 'TRANSFER' ? <ArrowLeftRight size={14} /> : t.type === 'ADJUST' ? <SlidersHorizontal size={14} /> : title(t).charAt(0).toUpperCase()}
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-1.5">
          <span className="truncate text-sm font-medium text-ink">{title(t)}</span>
          {badge && (
            <span title={badge.label} className="text-stone-400">
              <badge.Icon size={12} aria-label={badge.label} />
            </span>
          )}
          {!editable && t.type !== 'ADJUST' && (
            <span title="Gắn với khoản nợ / mục tiêu — sửa ở chức năng tương ứng" className="text-stone-400">
              <Lock size={11} />
            </span>
          )}
        </span>
        <span className="block truncate text-xs text-stone-500">{[t.note, meta].filter(Boolean).join(' · ') || ' '}</span>
      </span>
      <span className="tabular shrink-0 text-sm font-medium">
        <Amount t={t} />
      </span>
    </>
  )

  return (
    <li>
      {onClick && editable ? (
        <button onClick={onClick} className="flex w-full items-center gap-3 rounded-lg px-2 py-2.5 text-left transition-colors hover:bg-stone-50">
          {content}
        </button>
      ) : (
        <div className="flex items-center gap-3 px-2 py-2.5">{content}</div>
      )}
    </li>
  )
}
