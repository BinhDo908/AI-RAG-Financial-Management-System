import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Banknote, CalendarDays, ChevronDown, Info, Landmark, Pencil, Plus, Smartphone, Triangle } from 'lucide-react'
import { ApiError } from '../../api/client'
import { financeApi, type Budget, type ReportOverview, type Transaction } from '../../api/finance'
import type { Category, WalletList } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { currentMonth, formatCompact, formatMoney, formatMonth, formatSigned, monthRange, shiftMonth } from '../../lib/format'
import {
  BalanceLine,
  BudgetMeter,
  BudgetStatusLabel,
  CategoryBars,
  ChartCard,
  DataTable,
  SERIES,
  TrendColumns,
} from '../../components/finance/charts'
import { EmptyState, ErrorNote, FinancePage, PageHeader, Skeleton } from '../../components/finance/common'
import Mascot, { type MascotMood } from '../../components/mascot/Mascot'
import TransactionDialog from '../../components/finance/TransactionDialog'
import { TransactionRow } from '../../components/finance/TransactionRow'

type PeriodMode = 'month' | 'year'

function periodRange(mode: PeriodMode, month: string, year: string) {
  return mode === 'month' ? monthRange(month) : { from: `${year}-01-01`, to: `${year}-12-31` }
}

/** UC-A09: tổng quan tài chính theo kỳ — mascot, ví, chênh lệch thu chi, xu hướng, ngân sách, giao dịch gần đây. */
export default function OverviewPage() {
  const { user } = useAuth()
  const [mode, setMode] = useState<PeriodMode>('month')
  const [month, setMonth] = useState(currentMonth())
  const [year, setYear] = useState(currentMonth().slice(0, 4))
  const [report, setReport] = useState<ReportOverview | null>(null)
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [recent, setRecent] = useState<Transaction[]>([])
  const [wallets, setWallets] = useState<WalletList | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const load = useCallback(async () => {
    const { from, to } = periodRange(mode, month, year)
    // Kỳ hiện tại: tính đến hôm nay để số dư cuối kỳ khớp thực tế
    const current = mode === 'month' ? month === currentMonth() : year === currentMonth().slice(0, 4)
    const end = current ? undefined : to
    try {
      const [r, b, t] = await Promise.all([
        financeApi.overview(from, end),
        // Ngân sách đặt theo tháng: khi xem cả năm thì hiện của tháng này
        financeApi.budgets(mode === 'month' ? month : currentMonth()),
        financeApi.transactions({ from, to }, 0, 5),
      ])
      setReport(r)
      setBudgets(b)
      setRecent(t.page.items)
      setError(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được dữ liệu tổng quan')
    }
  }, [mode, month, year])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    void financeApi.wallets().then(setWallets).catch(() => undefined)
    void financeApi.categories().then(setCategories).catch(() => undefined)
  }, [])


  const mascot = useMemo(() => mascotFor(user?.fullName, report, budgets, error), [user?.fullName, report, budgets, error])

  return (
    <FinancePage>
      <PageHeader title="Tổng quan">
        <button className="btn btn-primary" onClick={() => setAdding(true)} disabled={!wallets?.wallets.length}>
          <Plus size={16} />
          <span className="sm:hidden">Thêm</span>
          <span className="hidden sm:inline">Thêm giao dịch</span>
        </button>
      </PageHeader>

      {error && <ErrorNote message={error} />}

      {/* Điện thoại xếp dọc; màn hình rộng: mascot bên trái, ví và thu chi bên phải */}
      <section className="grid grid-cols-[minmax(0,1fr)] gap-4 lg:grid-cols-[260px_minmax(0,1fr)] lg:items-center">
        <Mascot mood={mascot.mood} message={mascot.message} />
        <div className="grid min-w-0 grid-cols-[minmax(0,1fr)] gap-3">
          <WalletCards wallets={wallets} />
          <div className="flex flex-wrap items-center gap-2">
            <PeriodSelect label="Xem theo" value={mode} onChange={(v) => setMode(v as PeriodMode)} icon={<CalendarDays size={16} aria-hidden />}>
              <option value="month">Tháng</option>
              <option value="year">Năm</option>
            </PeriodSelect>
            {mode === 'month' ? (
              <PeriodSelect label="Chọn tháng" value={month} onChange={setMonth}>
                {Array.from({ length: 24 }, (_, i) => shiftMonth(currentMonth(), -i)).map((m) => (
                  <option key={m} value={m}>
                    {formatMonth(m)}
                  </option>
                ))}
              </PeriodSelect>
            ) : (
              <PeriodSelect label="Chọn năm" value={year} onChange={setYear}>
                {Array.from({ length: 5 }, (_, i) => String(Number(currentMonth().slice(0, 4)) - i)).map((y) => (
                  <option key={y} value={y}>
                    Năm {y}
                  </option>
                ))}
              </PeriodSelect>
            )}
          </div>
          <NetChangeCard report={report} />
        </div>
      </section>

      {report && (
        <div className="grid grid-cols-[minmax(0,1fr)] gap-4 lg:grid-cols-2">
          <ChartCard
            title="Thu và chi 6 tháng"
            legend={[SERIES.income, SERIES.expense]}
            table={
              <DataTable
                head={['Tháng', 'Thu', 'Chi', 'Chênh lệch']}
                rows={report.monthlyTrend.map((m) => [formatMonth(m.month), formatMoney(m.income), formatMoney(m.expense), formatSigned(m.income - m.expense)])}
              />
            }
          >
            <TrendColumns data={report.monthlyTrend} />
          </ChartCard>
          <ChartCard
            title="Số dư các ví"
            table={<DataTable head={['Ngày', 'Tổng số dư']} rows={report.balanceHistory.map((b) => [b.date.split('-').reverse().join('/'), formatMoney(b.totalBalance)])} />}
          >
            <BalanceLine data={report.balanceHistory} />
          </ChartCard>
        </div>
      )}

      {report && (
        <div className="grid grid-cols-[minmax(0,1fr)] gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <ChartCard
            title="Chi theo danh mục"
            table={
              <DataTable
                head={['Danh mục', 'Số tiền', 'Tỷ trọng']}
                rows={report.expenseByCategory.map((c) => [c.categoryName, formatMoney(c.amount), `${Math.round(c.percent)}%`])}
              />
            }
          >
            {report.expenseByCategory.length ? (
              <CategoryBars data={report.expenseByCategory} />
            ) : (
              <p className="py-10 text-center text-sm text-stone-500">Chưa có khoản chi nào trong tháng.</p>
            )}
          </ChartCard>

          <section className="min-w-0 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
            <SectionHeader title="Ngân sách" to="/budgets" linkLabel="Quản lý" />
            {budgets.length ? (
              <ul className="space-y-4">
                {[...budgets]
                  .sort((a, b) => b.percentUsed - a.percentUsed)
                  .slice(0, 5)
                  .map((b) => (
                    <li key={b.id}>
                      <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
                        <span className="truncate text-stone-700">{b.categoryName}</span>
                        <span className="tabular shrink-0 text-xs text-stone-500">
                          <span className="font-medium text-ink">{formatMoney(b.spent)}</span> / {formatMoney(b.limitAmount)}
                        </span>
                      </div>
                      <BudgetMeter percent={b.percentUsed} warnThreshold={b.warnThreshold} status={b.status} />
                      {b.status !== 'OK' && (
                        <div className="mt-1">
                          <BudgetStatusLabel status={b.status} />
                        </div>
                      )}
                    </li>
                  ))}
              </ul>
            ) : (
              <p className="py-8 text-center text-sm text-stone-500">Chưa có ngân sách tháng này.</p>
            )}
          </section>
        </div>
      )}

      {report && (
        <section className="min-w-0 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
          <SectionHeader title="Giao dịch gần đây" to="/transactions" linkLabel="Xem tất cả" />
          {recent.length ? (
            <ul className="-mx-2 divide-y divide-stone-100">
              {recent.map((t) => (
                <TransactionRow key={t.id} transaction={t} showDate />
              ))}
            </ul>
          ) : (
            <EmptyState
              title="Chưa có giao dịch trong tháng"
              action={
                <div className="flex gap-2">
                  <button className="btn btn-primary" onClick={() => setAdding(true)} disabled={!wallets?.wallets.length}>
                    <Plus size={16} /> Thêm giao dịch
                  </button>
                  <Link to="/chat" className="btn btn-secondary">
                    Mở trò chuyện
                  </Link>
                </div>
              }
            />
          )}
        </section>
      )}

      {adding && wallets && (
        <TransactionDialog
          wallets={wallets.wallets}
          categories={categories}
          onClose={() => setAdding(false)}
          onSaved={() => {
            setAdding(false)
            void load()
          }}
        />
      )}
    </FinancePage>
  )
}

/** Lời thoại của mascot theo dữ liệu trong kỳ; vượt ngân sách được ưu tiên hơn thu chi. */
function mascotFor(fullName: string | undefined, report: ReportOverview | null, budgets: Budget[], error: string | null): { mood: MascotMood; message: string } {
  if (error) return { mood: 'idle', message: 'Mình chưa tải được dữ liệu' }
  if (!report) return { mood: 'thinking', message: 'Đợi mình xem sổ sách nhé…' }
  const over = budgets.find((b) => b.status === 'OVER')
  if (over) return { mood: 'idle', message: `Ngân sách ${over.categoryName} đã vượt mức` }
  const warning = budgets.find((b) => b.status === 'WARNING')
  if (warning) return { mood: 'idle', message: `Ngân sách ${warning.categoryName} sắp hết` }
  if (report.net < 0) return { mood: 'idle', message: 'Kỳ này chi nhiều hơn thu rồi' }
  if (report.net > 0) return { mood: 'idle', message: `Kỳ này dư ${formatCompact(report.net)}` }
  const name = fullName?.trim().split(/\s+/).pop()
  return { mood: 'idle', message: name ? `Chào ${name} 👋` : 'Chào bạn 👋' }
}

const WALLET_ICONS = { CASH: Banknote, BANK: Landmark, EWALLET: Smartphone } as const

function WalletCards({ wallets }: { wallets: WalletList | null }) {
  if (!wallets) return <Skeleton className="h-[116px] rounded-2xl" />
  const active = wallets.wallets.filter((w) => !w.archived)
  return (
    <div className="-mx-4 flex snap-x gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none] sm:-mx-6 sm:px-6 lg:mx-0 lg:px-0">
      {active.map((w) => {
        const Icon = WALLET_ICONS[w.type]
        return (
          <Link
            key={w.id}
            to="/wallets"
            className={`relative flex h-[116px] w-44 shrink-0 snap-start flex-col items-center justify-center rounded-2xl border px-3 text-center transition-colors ${
              w.defaultWallet ? 'border-aqua-200 bg-aqua-50 hover:border-aqua-300' : 'border-stone-200 bg-white hover:border-stone-300'
            }`}
          >
            <Icon size={18} aria-hidden className="absolute top-3 left-3 text-ink" />
            <span aria-hidden className="absolute top-2.5 right-2.5 grid size-7 place-items-center rounded-full border border-stone-400 text-stone-500">
              <Pencil size={13} />
            </span>
            <span className="max-w-full truncate text-sm font-medium text-ink">{w.name}</span>
            <span className={`tabular max-w-full truncate text-[15px] font-semibold ${w.currentBalance < 0 ? 'text-negative' : 'text-ink'}`}>
              {formatMoney(w.currentBalance)}
            </span>
          </Link>
        )
      })}
      <Link
        to="/wallets"
        className="flex h-[116px] w-40 shrink-0 snap-start flex-col items-center justify-center gap-1 rounded-2xl bg-stone-200/70 text-stone-500 transition-colors hover:bg-stone-200 hover:text-ink"
      >
        <Plus size={22} aria-hidden />
        <span className="text-sm">Ví mới</span>
      </Link>
    </div>
  )
}

function PeriodSelect({ label, value, onChange, icon, children }: { label: string; value: string; onChange: (v: string) => void; icon?: ReactNode; children: ReactNode }) {
  return (
    <div className="relative inline-flex items-center">
      <select aria-label={label} value={value} onChange={(e) => onChange(e.target.value)} className={`period-select ${icon ? 'pr-16' : 'pr-9'}`}>
        {children}
      </select>
      <span className="pointer-events-none absolute right-2.5 flex items-center gap-1.5 text-ink">
        {icon}
        <ChevronDown size={16} aria-hidden />
      </span>
    </div>
  )
}

/** Thẻ chênh lệch thu chi trong kỳ; dòng cuối giữ tài sản ròng vì đây là số người dùng hay xem nhất. */
function NetChangeCard({ report }: { report: ReportOverview | null }) {
  const [showInfo, setShowInfo] = useState(false)
  return (
    <section className="net-card rounded-2xl border border-aqua-200 p-4 sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-[15px] font-semibold text-ink">Chênh lệch</h2>
          {report ? (
            <p className="tabular mt-0.5 truncate text-2xl font-semibold tracking-[-0.02em] text-ink">{report.net === 0 ? formatMoney(0) : formatSigned(report.net)}</p>
          ) : (
            <Skeleton className="mt-1 h-8 w-40 bg-white/60" />
          )}
        </div>
        <button onClick={() => setShowInfo((v) => !v)} aria-expanded={showInfo} aria-label="Giải thích chênh lệch" className="btn btn-ghost btn-icon shrink-0 text-stone-500">
          <Info size={18} />
        </button>
      </div>
      {showInfo && <p className="mt-1 text-xs text-stone-600">Tổng thu trừ tổng chi trong kỳ đang xem.</p>}

      <div className="mt-3 grid grid-cols-2 gap-2 sm:gap-3">
        <FlowTile label="Chi" value={report?.totalExpense} tone="text-negative" iconClass="rotate-180" />
        <FlowTile label="Thu" value={report?.totalIncome} tone="text-accent-700" />
      </div>

      {report && (
        <p className="tabular mt-3 flex flex-wrap justify-between gap-x-4 gap-y-0.5 text-xs text-stone-600">
          <span>
            Tài sản ròng <span className="font-semibold text-ink">{formatMoney(report.netWorth.netWorth)}</span>
          </span>
          {(report.netWorth.lending > 0 || report.netWorth.borrowing > 0) && (
            <span>
              Cho vay {formatMoney(report.netWorth.lending)} · Đi vay {formatMoney(report.netWorth.borrowing)}
            </span>
          )}
        </p>
      )}
    </section>
  )
}

function FlowTile({ label, value, tone, iconClass = '' }: { label: string; value: number | undefined; tone: string; iconClass?: string }) {
  return (
    <div className={`min-w-0 rounded-2xl bg-white/70 px-3 py-3 text-center ${tone}`}>
      <div className="text-sm">{label}</div>
      {value === undefined ? (
        <Skeleton className="mx-auto mt-1 h-5 w-20" />
      ) : (
        <div className="tabular mt-0.5 flex items-center justify-center gap-1.5 text-[15px] font-medium">
          <Triangle size={11} aria-hidden className={`shrink-0 fill-current ${iconClass}`} />
          <span className="truncate">{formatMoney(value)}</span>
        </div>
      )}
    </div>
  )
}

function SectionHeader({ title, to, linkLabel }: { title: string; to: string; linkLabel: string }) {
  return (
    <header className="mb-4 flex items-center justify-between gap-3">
      <h2 className="text-sm font-semibold text-ink">{title}</h2>
      <Link to={to} className="group inline-flex items-center gap-1 text-xs font-medium text-stone-500 hover:text-ink">
        {linkLabel}
        <ArrowRight size={13} className="transition-transform duration-150 group-hover:translate-x-0.5" />
      </Link>
    </header>
  )
}
