import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Copy, Plus, Trash2 } from 'lucide-react'
import { ApiError } from '../../api/client'
import { financeApi, type Budget, type BudgetStatus } from '../../api/finance'
import type { Category } from '../../api/types'
import { currentMonth, formatMoney, formatMonth, shiftMonth } from '../../lib/format'
import { BudgetMeter, BudgetStatusLabel } from '../../components/finance/charts'
import { EmptyState, ErrorNote, FinancePage, MoneyInput, MonthStepper, PageHeader, Skeleton } from '../../components/finance/common'
import Dialog from '../../components/ui/Dialog'

type DialogState = { kind: 'create' } | { kind: 'edit'; budget: Budget } | null

/** UC-A04: ngân sách theo danh mục chi trong tháng; cảnh báo khi chạm ngưỡng hoặc vượt. */
export default function BudgetsPage() {
  const [month, setMonth] = useState(currentMonth())
  const [budgets, setBudgets] = useState<Budget[] | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [dialog, setDialog] = useState<DialogState>(null)
  const [copying, setCopying] = useState(false)

  const load = useCallback(async () => {
    try {
      setBudgets(await financeApi.budgets(month))
      setError(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được ngân sách')
    }
  }, [month])

  useEffect(() => {
    setBudgets(null)
    void load()
  }, [load])

  useEffect(() => {
    void financeApi.categories().then(setCategories).catch(() => undefined)
  }, [])

  const copyPrevious = async () => {
    setCopying(true)
    try {
      await financeApi.copyPreviousBudgets(month)
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không sao chép được ngân sách tháng trước')
    } finally {
      setCopying(false)
    }
  }

  const done = () => {
    setDialog(null)
    void load()
  }

  const list = budgets ?? []
  const totalLimit = list.reduce((s, b) => s + b.limitAmount, 0)
  const totalSpent = list.reduce((s, b) => s + b.spent, 0)
  const totalPercent = totalLimit > 0 ? Math.floor((totalSpent / totalLimit) * 100) : 0
  const totalStatus: BudgetStatus = totalSpent >= totalLimit && totalLimit > 0 ? 'OVER' : totalPercent >= 80 ? 'WARNING' : 'OK'
  const counts = { OVER: list.filter((b) => b.status === 'OVER').length, WARNING: list.filter((b) => b.status === 'WARNING').length }
  const order: Record<BudgetStatus, number> = { OVER: 0, WARNING: 1, OK: 2 }
  const sorted = [...list].sort((a, b) => order[a.status] - order[b.status] || b.percentUsed - a.percentUsed)
  const usedCategoryIds = new Set(list.map((b) => b.categoryId))


  return (
    <FinancePage>
      <PageHeader title="Ngân sách">
        <MonthStepper value={month} onChange={setMonth} allowFuture />
        <button className="btn btn-primary" onClick={() => setDialog({ kind: 'create' })}>
          <Plus size={16} />
          Thêm
        </button>
      </PageHeader>

      {error && <ErrorNote message={error} />}

      {budgets === null ? (
        <>
          <Skeleton className="h-36" />
          <Skeleton className="h-64" />
        </>
      ) : list.length === 0 ? (
        <EmptyState
          title={`Chưa có ngân sách cho ${formatMonth(month).toLowerCase()}`}
          action={
            <div className="flex flex-wrap justify-center gap-2">
              <button className="btn btn-secondary" onClick={() => void copyPrevious()} disabled={copying}>
                <Copy size={15} />
                {copying ? 'Đang sao chép…' : `Sao chép từ ${formatMonth(shiftMonth(month, -1)).toLowerCase()}`}
              </button>
              <button className="btn btn-primary" onClick={() => setDialog({ kind: 'create' })}>
                <Plus size={16} /> Thêm ngân sách
              </button>
            </div>
          }
        />
      ) : (
        <>
          <section className="rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div className="min-w-0">
                <p className="text-sm text-stone-500">{totalLimit - totalSpent >= 0 ? 'Còn có thể chi' : 'Đã chi vượt'}</p>
                <p
                  className={`mt-1 truncate text-[clamp(2.25rem,11vw,3rem)] leading-none font-semibold tracking-[-0.03em] ${totalLimit - totalSpent < 0 ? 'text-negative' : 'text-ink'}`}
                >
                  {formatMoney(Math.abs(totalLimit - totalSpent))}
                </p>
              </div>
              <dl className="grid grid-cols-2 gap-x-6 text-sm sm:flex sm:gap-8">
                <div>
                  <dt className="text-xs text-stone-500">Đã chi</dt>
                  <dd className="tabular font-medium text-ink">{formatMoney(totalSpent)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-stone-500">Tổng hạn mức</dt>
                  <dd className="tabular font-medium text-ink">{formatMoney(totalLimit)}</dd>
                </div>
              </dl>
            </div>
            <div className="mt-5">
              <BudgetMeter percent={totalPercent} warnThreshold={80} status={totalStatus} height={10} />
              <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-stone-500">
                <span className="tabular">Đã dùng {totalPercent}%</span>
                <span>
                  {counts.OVER > 0 && <span className="text-negative">{counts.OVER} vượt hạn mức</span>}
                  {counts.OVER > 0 && counts.WARNING > 0 && ' · '}
                  {counts.WARNING > 0 && <span className="text-amber-700">{counts.WARNING} sắp hết</span>}
                  {counts.OVER + counts.WARNING === 0 && 'Mọi danh mục đều trong hạn mức'}
                </span>
              </div>
            </div>
          </section>

          <ul className="divide-y divide-stone-100 rounded-xl border border-stone-200 bg-white">
            {sorted.map((b) => (
              <li key={b.id}>
                <button onClick={() => setDialog({ kind: 'edit', budget: b })} className="block w-full px-4 py-3.5 text-left transition-colors hover:bg-stone-50">
                  <div className="mb-2 flex items-baseline justify-between gap-3">
                    <span className="truncate text-sm font-medium text-ink">{b.categoryName}</span>
                    <span className="tabular shrink-0 text-sm text-stone-500">
                      <span className="font-medium text-ink">{formatMoney(b.spent)}</span> / {formatMoney(b.limitAmount)}
                    </span>
                  </div>
                  <BudgetMeter percent={b.percentUsed} warnThreshold={b.warnThreshold} status={b.status} />
                  <div className="mt-1.5 flex items-center justify-between gap-3 text-xs">
                    <BudgetStatusLabel status={b.status} />
                    <span className="tabular text-stone-500">
                      {b.remaining >= 0 ? `Còn ${formatMoney(b.remaining)}` : `Vượt ${formatMoney(-b.remaining)}`} · {b.percentUsed}%
                    </span>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {dialog?.kind === 'create' && (
        <BudgetDialog
          month={month}
          categories={categories.filter((c) => c.type === 'EXPENSE' && !c.hidden && !usedCategoryIds.has(c.id))}
          onClose={() => setDialog(null)}
          onDone={done}
        />
      )}
      {dialog?.kind === 'edit' && <BudgetDialog month={month} budget={dialog.budget} categories={[]} onClose={() => setDialog(null)} onDone={done} />}
    </FinancePage>
  )
}

function BudgetDialog({
  month,
  budget,
  categories,
  onClose,
  onDone,
}: {
  month: string
  budget?: Budget
  categories: Category[]
  onClose: () => void
  onDone: () => void
}) {
  const editing = !!budget
  const [categoryId, setCategoryId] = useState<number | ''>(budget?.categoryId ?? '')
  const [limit, setLimit] = useState<number | null>(budget?.limitAmount ?? null)
  const [warn, setWarn] = useState(budget?.warnThreshold ?? 80)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setBusy(true)
    setError(null)
    try {
      await action()
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : fallback)
      setConfirmDelete(false)
    } finally {
      setBusy(false)
    }
  }

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!editing && !categoryId) return setError('Chọn danh mục')
    if (!limit) return setError('Nhập hạn mức lớn hơn 0')
    void run(
      () =>
        editing
          ? financeApi.updateBudget(budget!.id, { limitAmount: limit, warnThreshold: warn })
          : financeApi.createBudget({ categoryId: Number(categoryId), month, limitAmount: limit, warnThreshold: warn }),
      'Không lưu được ngân sách',
    )
  }

  return (
    <Dialog
      title={editing ? `${budget!.categoryName} · ${formatMonth(month)}` : `Thêm ngân sách · ${formatMonth(month)}`}
      onClose={onClose}
      footer={
        <>
          {editing && (
            <button
              type="button"
              className={`btn mr-auto ${confirmDelete ? 'btn-danger' : 'btn-ghost text-negative'}`}
              disabled={busy}
              onClick={() => (confirmDelete ? void run(() => financeApi.deleteBudget(budget!.id), 'Không xóa được ngân sách') : setConfirmDelete(true))}
            >
              <Trash2 size={15} />
              {confirmDelete ? 'Xác nhận xóa' : 'Xóa'}
            </button>
          )}
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Hủy
          </button>
          <button type="submit" form="budget-form" className="btn btn-primary" disabled={busy}>
            {busy ? 'Đang lưu…' : editing ? 'Lưu' : 'Thêm'}
          </button>
        </>
      }
    >
      <form id="budget-form" onSubmit={submit} className="grid gap-4" noValidate>
        {!editing && (
          <div>
            <label htmlFor="budget-category" className="label">
              Danh mục chi
            </label>
            <select id="budget-category" className="input" value={categoryId} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">Chọn danh mục</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {categories.length === 0 && <p className="mt-1 text-xs text-stone-500">Mọi danh mục chi đã có ngân sách tháng này.</p>}
          </div>
        )}
        <div>
          <label htmlFor="budget-limit" className="label">
            Hạn mức
          </label>
          <MoneyInput id="budget-limit" value={limit} onChange={setLimit} large />
          {editing && <p className="mt-1.5 text-xs text-stone-500">Đã chi {formatMoney(budget!.spent)} trong tháng.</p>}
        </div>
        <div>
          <div className="flex items-baseline justify-between">
            <label htmlFor="budget-warn" className="label">
              Ngưỡng cảnh báo
            </label>
            <span className="tabular text-sm font-medium text-ink">{warn}%</span>
          </div>
          <input
            id="budget-warn"
            type="range"
            min={50}
            max={95}
            step={5}
            value={warn}
            onChange={(e) => setWarn(Number(e.target.value))}
            className="w-full accent-[#1c1917]"
          />
          <p className="mt-1 text-xs text-stone-500">
            Nhắc khi chi tới {limit ? <span className="tabular">{formatMoney((limit * warn) / 100)}</span> : `${warn}% hạn mức`}.
          </p>
        </div>
        {error && <ErrorNote message={error} />}
      </form>
    </Dialog>
  )
}
