import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, FileClock, Flag, SearchX } from 'lucide-react'
import { ApiError } from '../../api/client'
import { adminApi, type QualityDashboard, type ReportSummary } from '../../api/admin'
import { knowledgeApi, type DocumentSummary } from '../../api/knowledge'
import type { FeedbackReason } from '../../api/types'
import { formatRelative } from '../../lib/format'
import { ChartCard, DataTable } from '../../components/finance/charts'
import { ErrorNote, FinancePage, PageHeader, Skeleton, StatTile } from '../../components/finance/common'
import Segmented from '../../components/ui/Segmented'

/* UC-C03 (chất lượng chatbot) làm trang chủ của Admin, kèm việc cần xử lý từ C01 (tài liệu nháp) và C02 (báo cáo). */

const RANGES = [
  { value: '7', label: '7 ngày' },
  { value: '30', label: '30 ngày' },
  { value: '90', label: '90 ngày' },
]

const INTENT_LABELS: Record<string, string> = {
  RECORD_TRANSACTION: 'Ghi giao dịch',
  QUERY_PERSONAL_DATA: 'Hỏi số liệu cá nhân',
  KNOWLEDGE_QA: 'Hỏi kiến thức (RAG)',
  PERSONAL_ADVICE: 'Tư vấn cá nhân hóa',
  MARKET_DATA: 'Tỷ giá, vàng, lãi suất',
  SMALL_TALK: 'Chào hỏi',
  OUT_OF_SCOPE: 'Ngoài phạm vi',
  CLARIFY: 'Hỏi lại cho rõ',
}

const REASON_LABELS: Record<FeedbackReason, string> = {
  WRONG_INFO: 'Sai thông tin',
  NO_SOURCE: 'Thiếu nguồn',
  IRRELEVANT: 'Không liên quan',
  WRONG_NUMBER: 'Sai số liệu',
  OTHER: 'Khác',
}

type Level = 'good' | 'warn' | 'bad'
const LEVEL = {
  good: { fill: '#2f8a5b', text: 'text-accent-700', label: 'Tốt' },
  warn: { fill: '#d97706', text: 'text-amber-700', label: 'Cần chú ý' },
  bad: { fill: '#b42318', text: 'text-negative', label: 'Kém' },
} as const

/** yyyy-MM-ddTHH:mm:ss theo giờ máy (backend nhận LocalDateTime). */
function localDateTime(d: Date) {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 19)
}

const pct = (ratio: number) => `${Math.round(ratio * 100)}%`

export default function AdminDashboardPage() {
  const [range, setRange] = useState('30')
  const [data, setData] = useState<QualityDashboard | null>(null)
  const [reports, setReports] = useState<{ items: ReportSummary[]; total: number } | null>(null)
  const [docs, setDocs] = useState<DocumentSummary[] | null>(null)
  const [users, setUsers] = useState<{ total: number; locked: number } | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    const to = new Date()
    const from = new Date(to.getTime() - Number(range) * 86400000)
    try {
      setData(await adminApi.dashboard(localDateTime(from), localDateTime(to)))
      setError(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được số liệu chatbot')
    }
  }, [range])

  useEffect(() => {
    void load()
  }, [load])

  // Việc cần xử lý không phụ thuộc khoảng thời gian
  useEffect(() => {
    void adminApi
      .openReports(5)
      .then((p) => setReports({ items: p.items, total: p.totalItems }))
      .catch(() => setReports({ items: [], total: 0 }))
    void knowledgeApi
      .list()
      .then((p) => setDocs(p.items))
      .catch(() => setDocs([]))
    void Promise.all([adminApi.users({ role: 'USER' }), adminApi.users({ role: 'USER', status: 'LOCKED' })])
      .then(([all, locked]) => setUsers({ total: all.totalItems, locked: locked.totalItems }))
      .catch(() => undefined)
  }, [])

  const draftDocs = docs?.filter((d) => d.status === 'DRAFT') ?? []
  const approvedDocs = docs?.filter((d) => d.status === 'APPROVED') ?? []
  const chunkTotal = approvedDocs.reduce((s, d) => s + d.chunkCount, 0)
  const chunkEmbedded = approvedDocs.reduce((s, d) => s + d.embeddedCount, 0)

  const intents = data
    ? Object.entries(data.intentDistribution)
        .filter(([, n]) => n > 0)
        .sort((a, b) => b[1] - a[1])
    : []
  const intentTotal = intents.reduce((s, [, n]) => s + n, 0)
  const ratings = data ? data.thumbsUp + data.thumbsDown : 0

  return (
    <FinancePage>
      <PageHeader title="Bảng điều khiển">
        <Segmented value={range} options={RANGES} onChange={setRange} label="Khoảng thời gian" />
      </PageHeader>

      {error && <ErrorNote message={error} />}

      {/* Việc cần xử lý: mỗi ô dẫn thẳng tới nơi xử lý */}
      <section className="grid grid-cols-[minmax(0,1fr)] gap-3 sm:grid-cols-3">
        <TodoTile
          icon={<FileClock size={16} />}
          count={docs ? draftDocs.length : null}
          label="Tài liệu chờ duyệt"
          to="/admin/knowledge"
          action="Mở kho tri thức"
        />
        <TodoTile
          icon={<SearchX size={16} />}
          count={data ? data.noSourceAnswers : null}
          label="Câu trả lời thiếu nguồn"
          to="#unanswered"
          action="Xem câu hỏi"
        />
        <TodoTile
          icon={<Flag size={16} />}
          count={reports ? reports.total : null}
          label="Báo cáo chờ kiểm duyệt"
          to="#reports"
          action="Xem báo cáo"
        />
      </section>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {data ? (
          <>
            <StatTile label="Người dùng" value={users ? `${data.activeUsers} / ${users.total}` : data.activeUsers.toLocaleString('vi-VN')} />
            <StatTile label="Hội thoại" value={data.conversations.toLocaleString('vi-VN')} />
            <StatTile label="Tin nhắn" value={data.userMessages.toLocaleString('vi-VN')} />
            <StatTile
              label="Phản hồi trung bình"
              value={data.averageLatencyMs !== null ? `${(data.averageLatencyMs / 1000).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} giây` : '—'}
            />
          </>
        ) : (
          [0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-[84px]" />)
        )}
      </section>

      {data && (
        <div className="grid grid-cols-[minmax(0,1fr)] gap-4 lg:grid-cols-2">
          <ChartCard
            title="Người dùng hỏi gì"
            table={<DataTable head={['Ý định', 'Số câu', 'Tỷ lệ']} rows={intents.map(([k, n]) => [INTENT_LABELS[k] ?? k, n, pct(n / intentTotal)])} />}
          >
            {intents.length ? (
              <ul className="space-y-3">
                {intents.map(([key, n]) => (
                  <li key={key}>
                    <div className="mb-1 flex items-baseline justify-between gap-3 text-sm">
                      <span className="truncate text-stone-700">{INTENT_LABELS[key] ?? key}</span>
                      <span className="shrink-0 text-xs text-stone-500">
                        <span className="tabular font-medium text-ink">{n.toLocaleString('vi-VN')}</span>
                        <span className="tabular ml-1.5 inline-block w-9 text-right">{pct(n / intentTotal)}</span>
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-stone-100">
                      <div className="meter-fill h-2 rounded-full bg-[#2a78d6]" style={{ width: `${(n / intents[0][1]) * 100}%` }} />
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="py-10 text-center text-sm text-stone-500">Chưa có tin nhắn nào trong khoảng này.</p>
            )}
          </ChartCard>

          <section className="min-w-0 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
            <h2 className="mb-4 text-sm font-semibold text-ink">Chất lượng câu trả lời</h2>
            <div className="space-y-5">
              <QualityMeter
                label="Hài lòng"
                ratio={data.thumbsUpRatio}
                level={ratings === 0 ? null : data.thumbsUpRatio >= 0.7 ? 'good' : data.thumbsUpRatio >= 0.5 ? 'warn' : 'bad'}
                detail={ratings ? `${data.thumbsUp} hài lòng · ${data.thumbsDown} không hài lòng` : 'Chưa có đánh giá'}
              />
              <QualityMeter
                label="Thiếu nguồn"
                ratio={data.noSourceRate}
                level={data.knowledgeAnswers === 0 ? null : data.noSourceRate <= 0.2 ? 'good' : data.noSourceRate <= 0.4 ? 'warn' : 'bad'}
                detail={`${data.noSourceAnswers}/${data.knowledgeAnswers} câu`}
              />
            </div>
            <div className="mt-6 border-t border-stone-100 pt-4">
              <div className="mb-3 flex items-baseline justify-between gap-3">
                <h3 className="text-xs font-medium text-stone-600">Lần đánh giá gần nhất</h3>
                {data.latestEvaluation && <span className="text-xs text-stone-500">{formatRelative(data.latestEvaluation.createdAt)}</span>}
              </div>
              {data.latestEvaluation ? (
                <dl className="grid grid-cols-3 gap-3">
                  <EvalNumber label="Tìm đúng tài liệu" value={pct(data.latestEvaluation.hitRate)} />
                  <EvalNumber label="Có trích dẫn" value={pct(data.latestEvaluation.citationRate)} />
                  <EvalNumber label="Trả lời đúng" value={pct(data.latestEvaluation.accuracy)} />
                </dl>
              ) : (
                <p className="text-sm text-stone-500">Chưa chạy bộ đánh giá nào.</p>
              )}
            </div>
          </section>
        </div>
      )}

      <div className="grid grid-cols-[minmax(0,1fr)] gap-4 lg:grid-cols-2">
        <section id="unanswered" className="min-w-0 scroll-mt-4 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
          <SectionHeader title="Câu hỏi chưa có nguồn" to="/admin/knowledge" linkLabel="Bổ sung tài liệu" />
          {!data ? (
            <Skeleton className="h-24" />
          ) : data.unansweredQuestions.length ? (
            <ul className="divide-y divide-stone-100">
              {data.unansweredQuestions.map((q) => (
                <li key={q} className="py-2.5 text-sm text-stone-700">
                  {q}
                </li>
              ))}
            </ul>
          ) : (
            <p className="py-6 text-center text-sm text-stone-500">Mọi câu hỏi kiến thức đều tìm được nguồn.</p>
          )}
        </section>

        <div className="grid min-w-0 gap-4">
          <section id="reports" className="min-w-0 scroll-mt-4 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
            <h2 className="mb-4 text-sm font-semibold text-ink">Báo cáo chờ kiểm duyệt</h2>
            {!reports ? (
              <Skeleton className="h-20" />
            ) : reports.items.length ? (
              <ul className="divide-y divide-stone-100">
                {reports.items.map((r) => (
                  <li key={r.id} className="flex items-start justify-between gap-3 py-2.5 text-sm">
                    <div className="min-w-0">
                      <span className="font-medium text-ink">{r.reason ? REASON_LABELS[r.reason] : 'Không rõ lý do'}</span>
                      {r.intent && <span className="ml-1.5 text-xs text-stone-500">· {INTENT_LABELS[r.intent] ?? r.intent}</span>}
                      {r.comment && <p className="mt-0.5 line-clamp-2 text-xs text-stone-500">“{r.comment}”</p>}
                    </div>
                    <span className="shrink-0 text-xs text-stone-500">{formatRelative(r.createdAt)}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="py-6 text-center text-sm text-stone-500">Không có báo cáo nào đang chờ.</p>
            )}
          </section>

          <section className="min-w-0 rounded-xl border border-stone-200 bg-white p-4 sm:p-5">
            <SectionHeader title="Kho tri thức" to="/admin/knowledge" linkLabel="Quản lý" />
            {!docs ? (
              <Skeleton className="h-16" />
            ) : (
              <>
                <dl className="grid grid-cols-3 gap-3">
                  <EvalNumber label="Đang dùng" value={String(approvedDocs.length)} />
                  <EvalNumber label="Chờ duyệt" value={String(draftDocs.length)} />
                  <EvalNumber label="Đã tắt" value={String(docs.length - approvedDocs.length - draftDocs.length)} />
                </dl>
                {chunkTotal > 0 && (
                  <div className="mt-4">
                    <div className="mb-1.5 flex justify-between text-xs text-stone-500">
                      <span>Đoạn đã tạo embedding</span>
                      <span className="tabular">
                        {chunkEmbedded}/{chunkTotal}
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-stone-100">
                      <div
                        className="meter-fill h-2 rounded-full"
                        style={{ width: `${(chunkEmbedded / chunkTotal) * 100}%`, background: chunkEmbedded === chunkTotal ? LEVEL.good.fill : LEVEL.warn.fill }}
                      />
                    </div>
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </div>
    </FinancePage>
  )
}

function TodoTile({ icon, count, label, to, action }: { icon: ReactNode; count: number | null; label: string; to: string; action: string }) {
  const active = (count ?? 0) > 0
  const body = (
    <div className="flex items-center gap-3">
      <span className={`grid size-9 shrink-0 place-items-center rounded-lg ${active ? 'bg-amber-50 text-amber-700' : 'bg-stone-100 text-stone-500'}`}>{icon}</span>
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium text-ink">{label}</div>
        {active && (
          <div className="inline-flex items-center gap-0.5 text-xs font-medium text-stone-600">
            {action} <ArrowRight size={12} className="transition-transform duration-150 group-hover:translate-x-0.5" />
          </div>
        )}
      </div>
      {count === null ? <Skeleton className="h-7 w-8" /> : <span className="tabular shrink-0 text-2xl font-semibold text-ink">{count}</span>}
    </div>
  )
  const cls = 'group block min-w-0 rounded-xl border border-stone-200 bg-white px-4 py-3 transition-colors hover:border-stone-300'
  if (!active) return <div className={cls.replace(' hover:border-stone-300', '')}>{body}</div>
  return to.startsWith('#') ? (
    <a href={to} className={cls}>
      {body}
    </a>
  ) : (
    <Link to={to} className={cls}>
      {body}
    </Link>
  )
}

function QualityMeter({ label, ratio, level, detail }: { label: string; ratio: number; level: Level | null; detail: string }) {
  const s = level ? LEVEL[level] : null
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
        <span className="text-stone-700">{label}</span>
        <span className="tabular font-semibold text-ink">{level ? pct(ratio) : '—'}</span>
      </div>
      <div className="h-2 rounded-full bg-stone-100">
        {s && <div className="meter-fill h-2 rounded-full" style={{ width: `${Math.max(ratio > 0 ? 2 : 0, ratio * 100)}%`, background: s.fill }} />}
      </div>
      <div className="mt-1 flex justify-between gap-3 text-xs">
        <span className="text-stone-500">{detail}</span>
        {s && <span className={`shrink-0 font-medium ${s.text}`}>{s.label}</span>}
      </div>
    </div>
  )
}

function EvalNumber({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="truncate text-xs text-stone-500">{label}</dt>
      <dd className="tabular text-lg font-semibold text-ink">{value}</dd>
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
