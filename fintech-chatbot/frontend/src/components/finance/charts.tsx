import { useState, type PointerEvent, type ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, CircleAlert, Table2, BarChart3 } from 'lucide-react'
import { useElementWidth } from '../../hooks/useElementWidth'
import { formatCompact, formatDate, formatMoney, formatMonth, formatSigned } from '../../lib/format'
import type { BudgetStatus, CategoryShare } from '../../api/finance'

/*
 * Biểu đồ vẽ tay bằng SVG theo quy tắc dataviz:
 * - Cặp màu thu/chi đã chạy validate_palette.js (CVD ΔE 20.4, tương phản ≥ 3:1 trên nền trắng).
 * - Một trục duy nhất; lưới mờ; cột ≤ 24px, đầu bo 4px bám đáy; đường 2px + vùng tô 10%.
 * - Chữ luôn dùng màu chữ, không dùng màu của chuỗi dữ liệu; luôn có chú thích khi ≥ 2 chuỗi + chế độ bảng.
 */
export const SERIES = {
  income: { color: '#2f8a5b', label: 'Thu' },
  expense: { color: '#2a78d6', label: 'Chi' },
  balance: { color: '#2a78d6', label: 'Số dư' },
}

const GRID = '#e7e5e4'
const AXIS_TEXT = '#78716c'

/** Vạch chia "đẹp" (1, 2, 5 × 10^n) bao trọn [min, max]. */
function niceTicks(min: number, max: number, count = 4): number[] {
  if (max === min) {
    max = min === 0 ? 1_000_000 : min + Math.abs(min) * 0.1
  }
  const raw = (max - min) / count
  // Bước tối thiểu 1đ: tránh vạch lẻ bị làm tròn trùng nhau
  const mag = Math.max(1, Math.pow(10, Math.floor(Math.log10(raw))))
  const step = [1, 2, 2.5, 5, 10].map((m) => m * mag).find((s) => s >= raw) ?? 10 * mag
  const start = Math.floor(min / step) * step
  const ticks: number[] = []
  for (let v = start; v <= max + step * 0.001; v += step) ticks.push(Math.round(v))
  if (ticks[ticks.length - 1] < max) ticks.push(ticks[ticks.length - 1] + step)
  return ticks
}

/** Cột bo tròn 4px ở đầu dữ liệu, đáy vuông bám trục. */
function topRoundedBar(x: number, y: number, w: number, h: number) {
  const r = Math.min(4, w / 2, h)
  return `M${x},${y + h}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h}Z`
}

/* ---------- Khung biểu đồ: tiêu đề, chú thích, chuyển sang bảng ---------- */

interface ChartCardProps {
  title: string
  legend?: { color: string; label: string }[]
  table: ReactNode
  children: ReactNode
  className?: string
}

export function ChartCard({ title, legend, table, children, className = '' }: ChartCardProps) {
  const [asTable, setAsTable] = useState(false)
  return (
    <section className={`min-w-0 rounded-xl border border-stone-200 bg-white p-4 sm:p-5 ${className}`}>
      <header className="mb-4 flex items-center justify-between gap-3">
        <h2 className="min-w-0 text-sm font-semibold text-ink">{title}</h2>
        <div className="flex shrink-0 items-center gap-3">
          {legend && !asTable && (
            <ul className="flex items-center gap-3 text-xs text-stone-600">
              {legend.map((l) => (
                <li key={l.label} className="flex items-center gap-1.5">
                  <span className="size-2.5 rounded-[3px]" style={{ background: l.color }} aria-hidden />
                  {l.label}
                </li>
              ))}
            </ul>
          )}
          <button
            onClick={() => setAsTable((v) => !v)}
            className="btn btn-ghost btn-icon btn-sm size-7"
            title={asTable ? 'Xem biểu đồ' : 'Xem dạng bảng'}
            aria-label={asTable ? 'Xem biểu đồ' : 'Xem dạng bảng'}
          >
            {asTable ? <BarChart3 size={14} /> : <Table2 size={14} />}
          </button>
        </div>
      </header>
      {asTable ? <div className="overflow-x-auto">{table}</div> : children}
    </section>
  )
}

export function DataTable({ head, rows }: { head: string[]; rows: (string | number)[][] }) {
  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-stone-200 text-left text-xs text-stone-500">
          {head.map((h, i) => (
            <th key={h} className={`py-2 font-medium ${i > 0 ? 'text-right' : ''}`}>
              {h}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="tabular">
        {rows.map((r, i) => (
          <tr key={i} className="border-b border-stone-100 last:border-0">
            {r.map((c, j) => (
              <td key={j} className={`py-2 ${j > 0 ? 'text-right text-stone-700' : 'text-stone-600'}`}>
                {c}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

/* ---------- Tooltip dùng chung ---------- */

function Tooltip({ x, width, children }: { x: number; width: number; children: ReactNode }) {
  // Lật sang trái khi gần mép phải để không tràn khung
  const flip = x > width - 180
  return (
    <div
      className="pointer-events-none absolute top-0 z-10 min-w-40 rounded-lg border border-stone-200 bg-white px-3 py-2 text-xs shadow-lg"
      style={{ left: flip ? undefined : x + 12, right: flip ? width - x + 12 : undefined }}
    >
      {children}
    </div>
  )
}

function TooltipRow({ color, label, value }: { color?: string; label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 py-0.5">
      <span className="flex items-center gap-1.5 whitespace-nowrap text-stone-500">
        {color && <span className="size-2 rounded-[2px]" style={{ background: color }} aria-hidden />}
        {label}
      </span>
      <span className="tabular font-medium text-ink">{value}</span>
    </div>
  )
}

/* ---------- Thu – chi theo tháng: cột nhóm ---------- */

const PAD = { top: 8, right: 4, bottom: 24, left: 44 }

export function TrendColumns({ data, height = 220 }: { data: { month: string; income: number; expense: number }[]; height?: number }) {
  const { ref, width } = useElementWidth<HTMLDivElement>()
  const [hover, setHover] = useState<number | null>(null)
  const max = Math.max(0, ...data.flatMap((d) => [d.income, d.expense]))
  const empty = max === 0
  const ticks = niceTicks(0, max)
  const top = ticks[ticks.length - 1]
  const plotW = Math.max(0, width - PAD.left - PAD.right)
  const plotH = height - PAD.top - PAD.bottom
  const groupW = data.length ? plotW / data.length : 0
  const barW = Math.max(4, Math.min(24, (groupW * 0.56 - 2) / 2))
  const y = (v: number) => PAD.top + plotH - (v / top) * plotH

  return (
    <div ref={ref} className="relative" style={{ height }} onMouseLeave={() => setHover(null)}>
      {empty && <p className="flex h-full items-center justify-center text-sm text-stone-500">Chưa có dữ liệu</p>}
      {width > 0 && !empty && (
        <svg width={width} height={height} role="img" aria-label="Biểu đồ cột thu và chi theo tháng">
          {ticks.map((t) => (
            <g key={t}>
              <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} stroke={GRID} strokeDasharray={t === 0 ? undefined : '2 3'} />
              <text x={PAD.left - 8} y={y(t)} dy="0.32em" textAnchor="end" fontSize={11} fill={AXIS_TEXT} className="tabular">
                {formatCompact(t)}
              </text>
            </g>
          ))}
          {data.map((d, i) => {
            const cx = PAD.left + groupW * i + groupW / 2
            const active = hover === null || hover === i
            return (
              <g key={d.month} opacity={active ? 1 : 0.35} style={{ transition: 'opacity 150ms ease' }}>
                {hover === i && <rect x={cx - groupW / 2 + 2} y={PAD.top} width={groupW - 4} height={plotH} rx={6} fill="#f5f5f4" />}
                {d.income > 0 && <path d={topRoundedBar(cx - barW - 1, y(d.income), barW, y(0) - y(d.income))} fill={SERIES.income.color} />}
                {d.expense > 0 && <path d={topRoundedBar(cx + 1, y(d.expense), barW, y(0) - y(d.expense))} fill={SERIES.expense.color} />}
                <text x={cx} y={height - 6} textAnchor="middle" fontSize={11} fill={hover === i ? '#1c1917' : AXIS_TEXT}>
                  {formatMonth(d.month, true)}
                </text>
                {/* Vùng bắt chuột rộng hơn cột */}
                <rect x={cx - groupW / 2} y={0} width={groupW} height={height} fill="transparent" onMouseEnter={() => setHover(i)} />
              </g>
            )
          })}
        </svg>
      )}
      {hover !== null && data[hover] && (
        <Tooltip x={PAD.left + groupW * hover + groupW / 2} width={width}>
          <div className="mb-1 font-medium text-ink">{formatMonth(data[hover].month)}</div>
          <TooltipRow color={SERIES.income.color} label="Thu" value={formatMoney(data[hover].income)} />
          <TooltipRow color={SERIES.expense.color} label="Chi" value={formatMoney(data[hover].expense)} />
          <div className="mt-1 border-t border-stone-100 pt-1">
            <TooltipRow label="Chênh lệch" value={formatSigned(data[hover].income - data[hover].expense)} />
          </div>
        </Tooltip>
      )}
    </div>
  )
}

/* ---------- Số dư theo thời gian: đường + vùng tô ---------- */

export function BalanceLine({ data, height = 220 }: { data: { date: string; totalBalance: number }[]; height?: number }) {
  const { ref, width } = useElementWidth<HTMLDivElement>()
  const [hover, setHover] = useState<number | null>(null)
  const values = data.map((d) => d.totalBalance)
  const empty = values.every((v) => v === 0)
  const lo = Math.min(0, ...values)
  const hi = Math.max(0, ...values)
  const ticks = niceTicks(lo, hi)
  const bottom = ticks[0]
  const top = ticks[ticks.length - 1]
  const inset = 14 // chừa chỗ cho nhãn trục x ở hai đầu
  const plotW = Math.max(0, width - PAD.left - PAD.right - inset * 2)
  const plotH = height - PAD.top - PAD.bottom
  const x = (i: number) => PAD.left + inset + (data.length > 1 ? (plotW * i) / (data.length - 1) : plotW / 2)
  const y = (v: number) => PAD.top + plotH - ((v - bottom) / (top - bottom)) * plotH
  const line = data.map((d, i) => `${i ? 'L' : 'M'}${x(i)},${y(d.totalBalance)}`).join('')
  const area = data.length ? `${line}L${x(data.length - 1)},${y(bottom)}L${x(0)},${y(bottom)}Z` : ''
  const last = data.length - 1

  const onMove = (e: PointerEvent<HTMLDivElement>) => {
    if (!data.length) return
    const rect = e.currentTarget.getBoundingClientRect()
    const px = e.clientX - rect.left
    const step = data.length > 1 ? plotW / (data.length - 1) : 1
    setHover(Math.max(0, Math.min(last, Math.round((px - PAD.left - inset) / step))))
  }

  return (
    <div ref={ref} className="relative" style={{ height }} onPointerMove={onMove} onPointerDown={onMove} onPointerLeave={() => setHover(null)}>
      {empty && <p className="flex h-full items-center justify-center text-sm text-stone-500">Chưa có dữ liệu</p>}
      {width > 0 && !empty && (
        <svg width={width} height={height} role="img" aria-label="Biểu đồ đường tổng số dư các ví">
          {ticks.map((t) => (
            <g key={t}>
              <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} stroke={GRID} strokeDasharray={t === 0 ? undefined : '2 3'} />
              <text x={PAD.left - 8} y={y(t)} dy="0.32em" textAnchor="end" fontSize={11} fill={AXIS_TEXT} className="tabular">
                {formatCompact(t)}
              </text>
            </g>
          ))}
          <path d={area} fill={SERIES.balance.color} fillOpacity={0.1} />
          <path d={line} fill="none" stroke={SERIES.balance.color} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />
          {data.map((d, i) => (
            <text key={d.date} x={x(i)} y={height - 6} textAnchor="middle" fontSize={11} fill={hover === i ? '#1c1917' : AXIS_TEXT}>
              {formatMonth(d.date.slice(0, 7), true)}
            </text>
          ))}
          {hover !== null && (
            <line x1={x(hover)} x2={x(hover)} y1={PAD.top} y2={PAD.top + plotH} stroke="#a8a29e" strokeDasharray="3 3" />
          )}
          {/* Điểm cuối (hoặc điểm đang trỏ): 8px, viền nền 2px */}
          {data.length > 0 && (
            <circle
              cx={x(hover ?? last)}
              cy={y(data[hover ?? last].totalBalance)}
              r={5}
              fill={SERIES.balance.color}
              stroke="#fff"
              strokeWidth={2}
            />
          )}
        </svg>
      )}
      {hover !== null && data[hover] && (
        <Tooltip x={x(hover)} width={width}>
          <div className="mb-1 font-medium text-ink">{formatDate(data[hover].date)}</div>
          <TooltipRow color={SERIES.balance.color} label="Tổng số dư" value={formatMoney(data[hover].totalBalance)} />
          {hover > 0 && <TooltipRow label="So với tháng trước" value={formatSigned(data[hover].totalBalance - data[hover - 1].totalBalance)} />}
        </Tooltip>
      )}
    </div>
  )
}

/* ---------- Chi theo danh mục: thanh ngang một màu, giá trị ở đầu thanh ---------- */

export function CategoryBars({ data, limit = 6 }: { data: CategoryShare[]; limit?: number }) {
  const sorted = [...data].sort((a, b) => b.amount - a.amount)
  // Chỉ gộp "Khác" khi còn từ 2 danh mục trở lên
  const cut = sorted.length > limit + 1 ? limit : sorted.length
  const shown = sorted.slice(0, cut)
  const rest = sorted.slice(cut)
  if (rest.length) {
    const amount = rest.reduce((s, c) => s + c.amount, 0)
    const percent = rest.reduce((s, c) => s + c.percent, 0)
    shown.push({ categoryId: -1, categoryName: `Khác (${rest.length} danh mục)`, amount, percent })
  }
  const max = Math.max(1, ...shown.map((c) => c.amount))
  return (
    <ul className="space-y-3">
      {shown.map((c) => (
        <li key={c.categoryId} className="group">
          <div className="mb-1 flex items-baseline justify-between gap-3 text-sm">
            <span className="truncate text-stone-700">{c.categoryName}</span>
            <span className="shrink-0 text-xs text-stone-500">
              <span className="tabular font-medium text-ink">{formatMoney(c.amount)}</span>
              <span className="tabular ml-1.5 inline-block w-9 text-right">{Math.round(c.percent)}%</span>
            </span>
          </div>
          <div className="h-2 rounded-full bg-stone-100">
            <div
              className="meter-fill h-2 rounded-full"
              style={{ width: `${(c.amount / max) * 100}%`, background: c.categoryId === -1 ? '#a8a29e' : SERIES.expense.color }}
            />
          </div>
        </li>
      ))}
    </ul>
  )
}

/* ---------- Thanh đo ngân sách: màu theo mức độ, luôn kèm biểu tượng + nhãn ---------- */

export const BUDGET_STATUS: Record<BudgetStatus, { fill: string; label: string; text: string; Icon: typeof CheckCircle2 }> = {
  OK: { fill: '#2f8a5b', label: 'Trong hạn mức', text: 'text-accent-700', Icon: CheckCircle2 },
  WARNING: { fill: '#d97706', label: 'Sắp hết', text: 'text-amber-700', Icon: AlertTriangle },
  OVER: { fill: '#b42318', label: 'Vượt hạn mức', text: 'text-negative', Icon: CircleAlert },
}

export function BudgetMeter({ percent, warnThreshold, status, height = 8 }: { percent: number; warnThreshold: number; status: BudgetStatus; height?: number }) {
  return (
    <div className="relative rounded-full bg-stone-100" style={{ height }}>
      <div
        className="meter-fill rounded-full"
        style={{ width: `${Math.min(100, Math.max(percent > 0 ? 2 : 0, percent))}%`, height, background: BUDGET_STATUS[status].fill }}
      />
      {/* Vạch ngưỡng cảnh báo */}
      <span
        className="absolute -top-0.5 w-px bg-stone-400"
        style={{ left: `${warnThreshold}%`, height: height + 4 }}
        title={`Ngưỡng cảnh báo ${warnThreshold}%`}
        aria-hidden
      />
    </div>
  )
}

export function BudgetStatusLabel({ status }: { status: BudgetStatus }) {
  const s = BUDGET_STATUS[status]
  return (
    <span className={`inline-flex items-center gap-1 text-xs font-medium ${s.text}`}>
      <s.Icon size={13} aria-hidden />
      {s.label}
    </span>
  )
}
