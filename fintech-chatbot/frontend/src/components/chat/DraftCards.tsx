import { useMemo, useState } from 'react'
import { Check, Loader2, X } from 'lucide-react'
import type { Category, TransactionDraft, TransactionRequest, Wallet } from '../../api/types'
import { formatMoney } from '../../lib/format'

interface Props {
  drafts: TransactionDraft[]
  categories: Category[]
  wallets: Wallet[]
  saved: boolean
  onSave: (transactions: TransactionRequest[]) => Promise<void>
}

type Item = TransactionDraft & { removed: boolean }

/**
 * UC-A11 bước 5–6 — Giao dịch nháp dạng dòng sổ cái: mô tả bên trái, số tiền bên phải (không bao giờ bị cắt).
 * User sửa từng trường (AF-3), bỏ bớt khoản, rồi bấm Lưu (BR-04).
 */
export default function DraftCards({ drafts, categories, wallets, saved, onSave }: Props) {
  const [items, setItems] = useState<Item[]>(() => drafts.map((d) => ({ ...d, removed: false })))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const activeWallets = useMemo(() => wallets.filter((w) => !w.archived), [wallets])
  const kept = items.filter((d) => !d.removed)
  const total = kept.reduce((sum, d) => sum + (d.type === 'INCOME' ? d.amount : -d.amount), 0)

  function update(index: number, patch: Partial<Item>) {
    setItems((prev) => prev.map((d, i) => (i === index ? { ...d, ...patch } : d)))
  }

  async function save() {
    setError(null)
    if (kept.some((d) => !d.amount || d.amount <= 0 || !d.walletId || !d.categoryId)) {
      setError('Mỗi khoản cần có số tiền, danh mục và ví.')
      return
    }
    setSaving(true)
    try {
      await onSave(
        kept.map((d) => ({
          type: d.type,
          amount: Math.round(d.amount),
          walletId: d.walletId!,
          categoryId: d.categoryId,
          txnDate: d.txnDate,
          note: d.note,
        })),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không lưu được giao dịch')
    } finally {
      setSaving(false)
    }
  }

  if (saved) {
    return (
      <div className="mt-3 overflow-hidden rounded-lg border border-stone-200 bg-white">
        {kept.map((d, i) => (
          <div key={i} className="flex items-center justify-between gap-4 border-b border-stone-100 px-4 py-2.5 last:border-0">
            <div className="min-w-0">
              <div className="truncate text-sm text-ink">{d.note || d.categoryName}</div>
              <div className="text-xs text-stone-500">
                {d.categoryName} · {d.walletName}
              </div>
            </div>
            <Amount value={d.amount} income={d.type === 'INCOME'} />
          </div>
        ))}
        <div className="flex items-center gap-1.5 border-t border-stone-100 bg-stone-50 px-4 py-2 text-xs text-accent-700">
          <Check size={13} /> Đã lưu vào sổ
        </div>
      </div>
    )
  }

  return (
    <div className="mt-3 overflow-hidden rounded-lg border border-stone-200 bg-white">
      {items.map((d, i) =>
        d.removed ? null : (
          <div key={i} className="border-b border-stone-100 px-4 py-3 last:border-0">
            <div className="flex items-center gap-3">
              <div className="flex shrink-0 rounded-md bg-stone-100 p-0.5 text-xs">
                {(['EXPENSE', 'INCOME'] as const).map((t) => (
                  <button
                    key={t}
                    onClick={() => update(i, { type: t, categoryId: null, categoryName: null })}
                    className={`rounded px-2 py-1 ${d.type === t ? 'bg-white font-medium text-ink shadow-sm' : 'text-stone-500'}`}
                  >
                    {t === 'EXPENSE' ? 'Chi' : 'Thu'}
                  </button>
                ))}
              </div>
              <input
                className="min-w-0 flex-1 border-0 bg-transparent px-0 text-ink outline-none placeholder:text-stone-400"
                value={d.note ?? ''}
                onChange={(e) => update(i, { note: e.target.value })}
                placeholder="Mô tả"
                maxLength={500}
                aria-label="Mô tả"
              />
              <div className="flex shrink-0 items-baseline gap-0.5">
                <span className={`text-sm ${d.type === 'INCOME' ? 'text-accent-700' : 'text-ink'}`}>{d.type === 'INCOME' ? '+' : '−'}</span>
                <input
                  className={`tabular w-28 border-0 bg-transparent px-0 text-right font-medium outline-none ${d.type === 'INCOME' ? 'text-accent-700' : 'text-ink'}`}
                  type="number"
                  min={1}
                  step={1000}
                  value={d.amount}
                  onChange={(e) => update(i, { amount: Number(e.target.value) })}
                  aria-label="Số tiền"
                />
                <span className="text-sm text-stone-400">đ</span>
              </div>
              <button
                className="flex size-7 shrink-0 items-center justify-center rounded text-stone-400 hover:bg-stone-100 hover:text-ink"
                title="Bỏ khoản này"
                aria-label="Bỏ khoản này"
                onClick={() => update(i, { removed: true })}
              >
                <X size={14} />
              </button>
            </div>
            <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <select
                className="input h-8"
                value={d.categoryId ?? ''}
                aria-label="Danh mục"
                onChange={(e) => {
                  const id = Number(e.target.value)
                  update(i, { categoryId: id, categoryName: categories.find((c) => c.id === id)?.name ?? null })
                }}
              >
                <option value="">Chọn danh mục</option>
                {categories
                  .filter((c) => c.type === d.type)
                  .map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
              </select>
              <select
                className="input h-8"
                value={d.walletId ?? ''}
                aria-label="Ví"
                onChange={(e) => {
                  const id = Number(e.target.value)
                  update(i, { walletId: id, walletName: activeWallets.find((w) => w.id === id)?.name ?? null })
                }}
              >
                <option value="">Chọn ví</option>
                {activeWallets.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </select>
              <input className="input h-8" type="date" value={d.txnDate} aria-label="Ngày" onChange={(e) => update(i, { txnDate: e.target.value })} />
            </div>
          </div>
        ),
      )}
      {kept.length > 0 ? (
        <div className="flex items-center gap-3 border-t border-stone-100 bg-stone-50 px-4 py-2.5">
          <button onClick={() => void save()} disabled={saving} className="btn btn-primary btn-sm">
            {saving ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
            {kept.length > 1 ? `Lưu ${kept.length} khoản` : 'Lưu'}
          </button>
          {error ? (
            <span className="text-xs text-negative">{error}</span>
          ) : (
            kept.length > 1 && (
              <span className="tabular ml-auto text-xs text-stone-500">
                Tổng <span className="font-medium text-ink">{(total >= 0 ? '+' : '−') + formatMoney(Math.abs(total))}</span>
              </span>
            )
          )}
        </div>
      ) : (
        <p className="px-4 py-3 text-sm text-stone-400">Đã bỏ tất cả các khoản.</p>
      )}
    </div>
  )
}

function Amount({ value, income }: { value: number; income: boolean }) {
  return (
    <span className={`tabular shrink-0 text-sm font-medium ${income ? 'text-accent-700' : 'text-ink'}`}>
      {income ? '+' : '−'}
      {formatMoney(value)}
    </span>
  )
}
