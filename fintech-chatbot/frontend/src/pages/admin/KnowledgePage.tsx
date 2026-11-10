import { useCallback, useEffect, useRef, useState } from 'react'
import { Plus, RefreshCw, Search } from 'lucide-react'
import { ApiError } from '../../api/client'
import { knowledgeApi, type DocumentDetail, type DocumentStatus, type DocumentSummary } from '../../api/knowledge'
import { formatRelative } from '../../lib/format'
import DocumentDetailPanel from '../../components/admin/DocumentDetailPanel'
import QueryTester from '../../components/admin/QueryTester'
import StatusBadge from '../../components/admin/StatusBadge'
import UploadPanel from '../../components/admin/UploadPanel'

type Panel = { kind: 'none' } | { kind: 'upload' } | { kind: 'detail'; detail: DocumentDetail }

const STATUS_FILTERS: { value: DocumentStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả' },
  { value: 'DRAFT', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'DISABLED', label: 'Đã tắt' },
]

/** UC-C01 — Quản lý kho tri thức RAG (Admin kiêm chuyên viên tài chính). */
export default function KnowledgePage() {
  const [docs, setDocs] = useState<DocumentSummary[]>([])
  const [status, setStatus] = useState<DocumentStatus | ''>('')
  const [keyword, setKeyword] = useState('')
  const [panel, setPanel] = useState<Panel>({ kind: 'none' })
  const [error, setError] = useState<string | null>(null)
  const [reindexing, setReindexing] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const panelKey = panel.kind === 'detail' ? `detail-${panel.detail.document.id}` : panel.kind

  // Màn hình hẹp: panel nằm dưới danh sách, cuộn tới để người dùng thấy ngay
  useEffect(() => {
    if (panelKey !== 'none' && !window.matchMedia('(min-width: 1280px)').matches) {
      panelRef.current?.scrollIntoView({ block: 'start' })
    }
  }, [panelKey])

  const load = useCallback(() => {
    knowledgeApi
      .list(status, keyword)
      .then((p) => setDocs(p.items))
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : 'Không tải được danh sách tài liệu.'))
  }, [status, keyword])

  useEffect(() => {
    const t = window.setTimeout(load, 250)
    return () => window.clearTimeout(t)
  }, [load])

  async function open(id: number) {
    setError(null)
    try {
      setPanel({ kind: 'detail', detail: await knowledgeApi.get(id) })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Không mở được tài liệu.')
    }
  }

  async function reindexAll() {
    if (!window.confirm('Tạo lại embedding cho mọi tài liệu đã duyệt? Chỉ cần khi đổi model embedding.')) return
    setReindexing(true)
    setNotice(null)
    try {
      const r = await knowledgeApi.reindex()
      setNotice(`Đã index lại ${r.documents} tài liệu, ${r.chunks} đoạn.`)
      load()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Index lại thất bại.')
    } finally {
      setReindexing(false)
    }
  }

  const selectedId = panel.kind === 'detail' ? panel.detail.document.id : null
  const approved = docs.filter((d) => d.status === 'APPROVED')
  const indexedChunks = approved.reduce((s, d) => s + d.embeddedCount, 0)
  const showPanel = panel.kind !== 'none'

  return (
    <div className="h-full overflow-y-auto bg-stone-50">
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <h1 className="text-xl font-semibold tracking-[-0.02em] text-ink">Kho tri thức</h1>
          <div className="flex gap-2">
            <button onClick={() => void reindexAll()} disabled={reindexing || approved.length === 0} className="btn btn-secondary">
              <RefreshCw size={14} className={reindexing ? 'animate-spin' : ''} /> Index lại
            </button>
            <button onClick={() => setPanel({ kind: 'upload' })} className="btn btn-primary">
              <Plus size={15} /> Thêm tài liệu
            </button>
          </div>
        </div>

        <dl className="mt-6 flex gap-8 text-sm">
          <Stat label="Tài liệu" value={docs.length} />
          <Stat label="Đã duyệt" value={approved.length} />
          <Stat label="Đoạn đã index" value={indexedChunks} />
        </dl>

        {(error || notice) && <p className={`mt-4 text-sm ${error ? 'text-negative' : 'text-accent-700'}`}>{error ?? notice}</p>}

        <div className={`mt-6 grid grid-cols-[minmax(0,1fr)] gap-6 ${showPanel ? 'xl:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)]' : ''}`}>
          <div className="min-w-0 space-y-6">
            <section className="overflow-hidden rounded-lg border border-stone-200 bg-white">
              <div className="flex flex-wrap items-center gap-3 border-b border-stone-200 px-4 py-2.5">
                <div className="relative min-w-48 flex-1">
                  <Search size={14} className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-stone-400" />
                  <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm theo tiêu đề" className="input h-8 pl-8" />
                </div>
                <div className="-mx-1 flex gap-1 overflow-x-auto px-1 text-sm whitespace-nowrap [scrollbar-width:none]" role="tablist">
                  {STATUS_FILTERS.map((f) => (
                    <button
                      key={f.value}
                      role="tab"
                      aria-selected={status === f.value}
                      onClick={() => setStatus(f.value)}
                      className={`rounded-md px-2.5 py-1 transition-colors ${status === f.value ? 'bg-stone-100 font-medium text-ink' : 'text-stone-500 hover:text-ink'}`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>

              {docs.length === 0 ? (
                <p className="px-4 py-12 text-center text-sm text-stone-400">
                  {keyword || status ? 'Không có tài liệu phù hợp.' : 'Chưa có tài liệu. Bắt đầu bằng “Thêm tài liệu”.'}
                </p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-stone-200 text-left text-xs text-stone-500">
                      <th className="px-4 py-2 font-medium">Tài liệu</th>
                      <th className={`px-4 py-2 font-medium ${showPanel ? 'hidden 2xl:table-cell' : 'hidden md:table-cell'}`}>Nguồn</th>
                      <th className="px-4 py-2 text-right font-medium">Đoạn</th>
                      <th className="px-4 py-2 font-medium">Trạng thái</th>
                      <th className={`px-4 py-2 font-medium ${showPanel ? 'hidden' : 'hidden lg:table-cell'}`}>Cập nhật</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-100">
                    {docs.map((d) => (
                      <tr
                        key={d.id}
                        onClick={() => void open(d.id)}
                        className={`cursor-pointer transition-colors ${d.id === selectedId ? 'bg-stone-100' : 'hover:bg-stone-50'}`}
                      >
                        <td className="max-w-0 px-4 py-3">
                          <div className="line-clamp-2 text-ink" title={d.title}>
                            {d.title}
                          </div>
                          <div className="text-xs text-stone-400">{d.language === 'vi' ? 'Tiếng Việt' : 'Tiếng Anh'}</div>
                        </td>
                        <td className={`px-4 py-3 text-stone-500 ${showPanel ? 'hidden 2xl:table-cell' : 'hidden md:table-cell'}`}>{d.sourceId}</td>
                        <td className="tabular px-4 py-3 text-right whitespace-nowrap text-stone-600">
                          {d.status === 'APPROVED' && d.embeddedCount < d.chunkCount ? (
                            <span className="text-amber-700" title="Cần index lại">
                              {d.embeddedCount}/{d.chunkCount}
                            </span>
                          ) : (
                            d.chunkCount
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <StatusBadge status={d.status} />
                        </td>
                        <td className={`px-4 py-3 whitespace-nowrap text-stone-500 ${showPanel ? 'hidden' : 'hidden lg:table-cell'}`}>
                          {formatRelative(d.updatedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
            <QueryTester />
          </div>

          <div ref={panelRef} className={`min-w-0 scroll-mt-4 ${showPanel ? '' : 'hidden'}`}>
          {panel.kind === 'upload' && (
            <UploadPanel
              onClose={() => setPanel({ kind: 'none' })}
              onCreated={(detail) => {
                setPanel({ kind: 'detail', detail })
                load()
              }}
            />
          )}
          {panel.kind === 'detail' && (
            <DocumentDetailPanel
              key={panel.detail.document.id}
              detail={panel.detail}
              onClose={() => setPanel({ kind: 'none' })}
              onChanged={(detail) => {
                setPanel({ kind: 'detail', detail })
                load()
              }}
              onDeleted={() => {
                setPanel({ kind: 'none' })
                load()
              }}
            />
          )}
          </div>
        </div>
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <dt className="text-xs text-stone-500">{label}</dt>
      <dd className="tabular mt-0.5 text-xl font-semibold tracking-[-0.01em] text-ink">{value.toLocaleString('vi-VN')}</dd>
    </div>
  )
}
