import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Download, Plus, Search, X } from 'lucide-react'
import { ApiError } from '../../api/client'
import { financeApi, type Transaction, type TransactionFilter, type TransactionResult, type TransactionSearch } from '../../api/finance'
import type { Category, TransactionType, Wallet } from '../../api/types'
import { currentMonth, formatDayHeading, formatMoney, formatSigned, monthRange } from '../../lib/format'
import { EmptyState, ErrorNote, FinancePage, MonthStepper, PageHeader, Skeleton, StatTile } from '../../components/finance/common'
import { TransactionRow } from '../../components/finance/TransactionRow'
import TransactionDialog from '../../components/finance/TransactionDialog'
import Segmented from '../../components/ui/Segmented'

type TypeFilter = 'ALL' | Exclude<TransactionType, 'ADJUST'>
const PAGE_SIZE = 30

/** UC-A02 (thêm/sửa/xóa), A08 (tìm kiếm, lọc), A10 (xuất file). */
export default function TransactionsPage() {
  const [month, setMonth] = useState(currentMonth())
  const [type, setType] = useState<TypeFilter>('ALL')
  const [walletId, setWalletId] = useState<number | ''>('')
  const [categoryId, setCategoryId] = useState<number | ''>('')
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')

  const [result, setResult] = useState<TransactionSearch | null>(null)
  const [items, setItems] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [wallets, setWallets] = useState<Wallet[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [dialog, setDialog] = useState<{ transaction: Transaction | null } | null>(null)

  // Gõ tìm kiếm: chờ 300ms sau phím cuối mới gọi API
  useEffect(() => {
    const id = setTimeout(() => setKeyword(keywordInput.trim()), 300)
    return () => clearTimeout(id)
  }, [keywordInput])

  const filter = useMemo<TransactionFilter>(
    () => ({
      ...monthRange(month),
      type: type === 'ALL' ? undefined : type,
      walletId: walletId || undefined,
      categoryId: categoryId || undefined,
      keyword: keyword || undefined,
    }),
    [month, type, walletId, categoryId, keyword],
  )

  const requestId = useRef(0)
  const load = useCallback(
    async (page = 0) => {
      const id = ++requestId.current
      setLoading(true)
      try {
        const r = await financeApi.transactions(filter, page, PAGE_SIZE)
        if (id !== requestId.current) return // bỏ kết quả của bộ lọc cũ
        setResult(r)
        setItems((prev) => (page === 0 ? r.page.items : [...prev, ...r.page.items]))
        setError(null)
      } catch (err) {
        if (id === requestId.current) setError(err instanceof ApiError ? err.message : 'Không tải được giao dịch')
      } finally {
        if (id === requestId.current) setLoading(false)
      }
    },
    [filter],
  )

  useEffect(() => {
    void load(0)
  }, [load])

  useEffect(() => {
    void financeApi.wallets().then((w) => setWallets(w.wallets)).catch(() => undefined)
    void financeApi.categories().then(setCategories).catch(() => undefined)
  }, [])

  const groups = useMemo(() => {
    const map = new Map<string, Transaction[]>()
    items.forEach((t) => map.set(t.txnDate, [...(map.get(t.txnDate) ?? []), t]))
    return [...map.entries()]
  }, [items])

  const filtered = type !== 'ALL' || walletId !== '' || categoryId !== '' || keyword !== ''
  const clearFilters = () => {
    setType('ALL')
    setWalletId('')
    setCategoryId('')
    setKeywordInput('')
    setKeyword('')
  }

  const onSaved = (r: TransactionResult | null) => {
    setDialog(null)
    if (r?.negativeBalance) setNotice(`Lưu ý: ví ${r.transaction.walletName} đang âm (${formatMoney(r.walletBalance)}).`)
    else setNotice(null)
    void load(0)
  }

  const visibleCategories = categories.filter((c) => !c.hidden && (type === 'ALL' || type === 'TRANSFER' || c.type === type))

  return (
    <FinancePage>
      <PageHeader title="Giao dịch">
        <MonthStepper value={month} onChange={setMonth} />
        <ExportMenu month={month} />
        <button className="btn btn-primary" onClick={() => setDialog({ transaction: null })} disabled={!wallets.length}>
          <Plus size={16} />
          Thêm
        </button>
      </PageHeader>

      {/* Bộ lọc: một hàng, phía trên dữ liệu */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="relative min-w-0 flex-1 basis-56">
          <Search size={15} className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-stone-400" />
          <input
            className="input pl-9"
            placeholder="Tìm theo ghi chú…"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            aria-label="Tìm theo ghi chú"
          />
        </div>
        <Segmented
          value={type}
          onChange={(t) => {
            setType(t)
            if (t !== 'ALL' && categories.find((c) => c.id === categoryId)?.type !== t) setCategoryId('')
          }}
          label="Loại giao dịch"
          options={[
            { value: 'ALL', label: 'Tất cả' },
            { value: 'EXPENSE', label: 'Chi' },
            { value: 'INCOME', label: 'Thu' },
            { value: 'TRANSFER', label: 'Chuyển' },
          ]}
        />
        <select className="input w-auto min-w-0 flex-1 basis-36 sm:flex-none" value={walletId} onChange={(e) => setWalletId(e.target.value ? Number(e.target.value) : '')} aria-label="Lọc theo ví">
          <option value="">Mọi ví</option>
          {wallets.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
            </option>
          ))}
        </select>
        <select
          className="input w-auto min-w-0 flex-1 basis-36 sm:flex-none"
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
          disabled={type === 'TRANSFER'}
          aria-label="Lọc theo danh mục"
        >
          <option value="">Mọi danh mục</option>
          {visibleCategories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {filtered && (
          <button className="btn btn-ghost btn-sm" onClick={clearFilters}>
            <X size={14} />
            Bỏ lọc
          </button>
        )}
      </div>

      <section className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {result ? (
          <>
            <StatTile label="Tổng thu" value={formatMoney(result.totalIncome)} />
            <StatTile label="Tổng chi" value={formatMoney(result.totalExpense)} />
            <StatTile className="col-span-2 sm:col-span-1" label="Chênh lệch" value={formatSigned(result.net)} tone={result.net > 0 ? 'positive' : result.net < 0 ? 'negative' : 'neutral'} />
          </>
        ) : (
          [0, 1, 2].map((i) => <Skeleton key={i} className={`h-[68px] ${i === 2 ? 'col-span-2 sm:col-span-1' : ''}`} />)
        )}
      </section>

      {notice && (
        <p className="flex items-start justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {notice}
          <button onClick={() => setNotice(null)} aria-label="Đóng thông báo" className="text-amber-700 hover:text-amber-900">
            <X size={15} />
          </button>
        </p>
      )}
      {error && <ErrorNote message={error} />}

      {result && items.length === 0 && !loading ? (
        filtered ? (
          <EmptyState title="Không có giao dịch khớp bộ lọc" action={<button className="btn btn-secondary" onClick={clearFilters}>Bỏ lọc</button>} />
        ) : (
          <EmptyState
            title="Chưa có giao dịch trong tháng"
            action={
              <button className="btn btn-primary" onClick={() => setDialog({ transaction: null })} disabled={!wallets.length}>
                <Plus size={16} /> Thêm giao dịch
              </button>
            }
          />
        )
      ) : (
        <div className={`grid gap-4 transition-opacity ${loading && items.length ? 'opacity-60' : ''}`}>
          {!result && [0, 1].map((i) => <Skeleton key={i} className="h-40" />)}
          {groups.map(([date, list]) => {
            const dayNet = list.reduce((s, t) => s + (t.type === 'INCOME' ? t.amount : t.type === 'EXPENSE' ? -t.amount : 0), 0)
            return (
              <section key={date} className="min-w-0 rounded-xl border border-stone-200 bg-white px-2 pt-2 pb-1">
                <header className="flex items-baseline justify-between px-2 pt-1 pb-1.5 text-xs">
                  <h2 className="font-medium text-stone-600">{formatDayHeading(date)}</h2>
                  <span className="tabular text-stone-500">{dayNet !== 0 ? formatSigned(dayNet) : ''}</span>
                </header>
                <ul className="divide-y divide-stone-100">
                  {list.map((t) => (
                    <TransactionRow key={t.id} transaction={t} onClick={() => setDialog({ transaction: t })} />
                  ))}
                </ul>
              </section>
            )
          })}
          {result && result.page.page + 1 < result.page.totalPages && (
            <button className="btn btn-secondary mx-auto" disabled={loading} onClick={() => void load(result.page.page + 1)}>
              {loading ? 'Đang tải…' : `Tải thêm (${result.page.totalItems - items.length} giao dịch)`}
            </button>
          )}
        </div>
      )}

      {dialog && (
        <TransactionDialog
          wallets={wallets}
          categories={categories}
          transaction={dialog.transaction}
          onClose={() => setDialog(null)}
          onSaved={onSaved}
        />
      )}
    </FinancePage>
  )
}

/** UC-A10: xuất giao dịch của tháng đang xem. */
function ExportMenu({ month }: { month: string }) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false)
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [open])

  const run = async (format: 'CSV' | 'XLSX') => {
    setBusy(true)
    setError(null)
    try {
      const { from, to } = monthRange(month)
      await financeApi.exportTransactions(from, to, format)
      setOpen(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không xuất được file')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div ref={ref} className="relative">
      <button className="btn btn-secondary" onClick={() => setOpen((v) => !v)} aria-expanded={open} aria-haspopup="menu">
        <Download size={15} />
        <span className="hidden sm:inline">Xuất file</span>
      </button>
      {open && (
        <div role="menu" className="enter absolute top-full right-0 z-20 mt-1 w-56 origin-top-right rounded-lg border border-stone-200 bg-white p-1 shadow-lg">
          <button role="menuitem" disabled={busy} onClick={() => void run('XLSX')} className="w-full rounded-md px-3 py-2 text-left text-sm hover:bg-stone-50">
            Excel (.xlsx)
          </button>
          <button role="menuitem" disabled={busy} onClick={() => void run('CSV')} className="w-full rounded-md px-3 py-2 text-left text-sm hover:bg-stone-50">
            CSV (.csv)
          </button>
          {error && <p className="px-3 py-1.5 text-xs text-negative">{error}</p>}
        </div>
      )}
    </div>
  )
}
