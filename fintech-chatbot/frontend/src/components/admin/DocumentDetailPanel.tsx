import { useEffect, useRef, useState } from 'react'
import { ArrowUpRight, Loader2, X } from 'lucide-react'
import { ApiError } from '../../api/client'
import { knowledgeApi, OPEN_SOURCES, type DocumentDetail, type DocumentMetadata } from '../../api/knowledge'
import { formatRelative } from '../../lib/format'
import MetadataForm from './MetadataForm'
import StatusBadge from './StatusBadge'

interface Props {
  detail: DocumentDetail
  onChanged: (detail: DocumentDetail) => void
  onDeleted: () => void
  onClose: () => void
}

/**
 * UC-C01 bước 6–7 và AF-1, AF-2, AF-4 — Xem trước các đoạn, sửa metadata, duyệt (tạo embedding),
 * tắt, thay nội dung bằng phiên bản mới, xóa.
 */
export default function DocumentDetailPanel({ detail, onChanged, onDeleted, onClose }: Props) {
  const doc = detail.document
  const [metadata, setMetadata] = useState<DocumentMetadata>(toMetadata(detail))
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const replaceInput = useRef<HTMLInputElement>(null)

  useEffect(() => {
    setMetadata(toMetadata(detail))
    setError(null)
  }, [detail])

  async function run(action: string, fn: () => Promise<void>, success?: string) {
    setBusy(action)
    setError(null)
    setNotice(null)
    try {
      await fn()
      if (success) setNotice(success)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Thao tác thất bại.')
    } finally {
      setBusy(null)
    }
  }

  const reload = async () => onChanged(await knowledgeApi.get(doc.id))
  const source = OPEN_SOURCES.find((s) => s.id === doc.sourceId)
  const needsReindex = doc.status === 'APPROVED' && doc.embeddedCount < doc.chunkCount
  const canApprove = doc.status !== 'APPROVED' || needsReindex
  const spin = (action: string) => busy === action && <Loader2 size={14} className="animate-spin" />

  return (
    <section className="enter flex flex-col rounded-lg border border-stone-200 bg-white">
      <header className="flex items-start justify-between gap-4 border-b border-stone-200 px-5 py-4">
        <div className="min-w-0">
          <h2 className="font-semibold tracking-[-0.01em] text-ink">{doc.title}</h2>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-stone-500">
            <StatusBadge status={doc.status} />
            <span>
              {doc.sourceId} · {source?.name ?? 'Nguồn khác'}
            </span>
            <span className="tabular">
              {doc.embeddedCount}/{doc.chunkCount} đoạn đã index
            </span>
            <span>cập nhật {formatRelative(doc.updatedAt)}</span>
            {doc.url && (
              <a href={doc.url} target="_blank" rel="noreferrer noopener" className="inline-flex items-center gap-0.5 underline decoration-stone-300 underline-offset-2 hover:text-ink">
                Mở nguồn <ArrowUpRight size={11} />
              </a>
            )}
          </div>
        </div>
        <button onClick={onClose} className="btn btn-ghost btn-icon btn-sm shrink-0" title="Đóng" aria-label="Đóng">
          <X size={16} />
        </button>
      </header>

      <div className="flex flex-wrap items-center gap-2 border-b border-stone-200 bg-stone-50 px-5 py-3">
        {canApprove ? (
          <button
            className="btn btn-primary"
            disabled={busy !== null}
            onClick={() =>
              void run('approve', async () => {
                await knowledgeApi.approve(doc.id)
                await reload()
              }, 'Đã duyệt. Trợ lý bắt đầu dùng tài liệu này.')
            }
          >
            {spin('approve')}
            {doc.status === 'DISABLED' ? 'Bật lại và index' : needsReindex ? 'Index lại' : 'Duyệt và tạo embedding'}
          </button>
        ) : (
          <button
            className="btn btn-secondary"
            disabled={busy !== null}
            onClick={() =>
              void run('disable', async () => {
                await knowledgeApi.disable(doc.id)
                await reload()
              }, 'Đã tắt. Trợ lý không còn truy xuất tài liệu này.')
            }
          >
            {spin('disable')}
            Tắt tài liệu
          </button>
        )}
        <button className="btn btn-secondary" disabled={busy !== null} onClick={() => replaceInput.current?.click()}>
          {spin('replace')}
          Cập nhật phiên bản mới
        </button>
        <input
          ref={replaceInput}
          type="file"
          accept=".pdf,.docx,.txt,.md,.html,.htm"
          className="sr-only"
          onChange={(e) => {
            const f = e.target.files?.[0]
            e.target.value = ''
            if (f) {
              void run('replace', async () => onChanged(await knowledgeApi.replaceContent(doc.id, f, f.name)),
                'Đã thay nội dung. Tài liệu chờ duyệt lại.')
            }
          }}
        />
        <button
          className="btn btn-danger ml-auto"
          disabled={busy !== null}
          onClick={() => {
            if (window.confirm('Xóa vĩnh viễn tài liệu, các đoạn và vector?')) {
              void run('delete', async () => {
                await knowledgeApi.remove(doc.id)
                onDeleted()
              })
            }
          }}
        >
          {spin('delete')}
          Xóa
        </button>
      </div>

      <div className="space-y-8 p-5">
        {(notice || error || doc.status === 'DRAFT') && (
          <p className={`text-sm ${error ? 'text-negative' : notice ? 'text-accent-700' : 'text-stone-600'}`}>
            {error ??
              notice ??
              'Xem lại các đoạn bên dưới (đã bỏ mục lục, số trang, header/footer lặp lại). Khi duyệt, hệ thống tạo embedding và trợ lý bắt đầu dùng tài liệu.'}
          </p>
        )}

        <div>
          <h3 className="mb-3 text-sm font-semibold text-ink">Metadata</h3>
          <MetadataForm value={metadata} onChange={setMetadata} />
          <div className="mt-3 flex justify-end">
            <button
              className="btn btn-secondary btn-sm"
              disabled={busy !== null}
              onClick={() =>
                void run('save', async () => {
                  await knowledgeApi.updateMetadata(doc.id, metadata)
                  await reload()
                }, 'Đã lưu metadata.')
              }
            >
              {spin('save')}
              Lưu metadata
            </button>
          </div>
        </div>

        <div>
          <h3 className="mb-3 text-sm font-semibold text-ink">
            Các đoạn <span className="tabular font-normal text-stone-400">{detail.chunks.length}</span>
          </h3>
          <ol className="divide-y divide-stone-100 border-y border-stone-100">
            {detail.chunks.map((c) => (
              <li key={c.id} className="py-3">
                <div className="mb-1 flex items-baseline gap-3 text-xs text-stone-400">
                  <span className="tabular w-6 shrink-0">{c.index + 1}</span>
                  <span className="min-w-0 truncate font-medium text-stone-700">{c.heading ?? 'Không có đề mục'}</span>
                  {c.pageNumber != null && <span className="shrink-0">trang {c.pageNumber}</span>}
                  <span className="tabular ml-auto shrink-0">{c.length.toLocaleString('vi-VN')} ký tự</span>
                </div>
                <p className="line-clamp-4 pl-9 text-sm whitespace-pre-wrap text-stone-600">{c.content}</p>
              </li>
            ))}
          </ol>
        </div>
      </div>
    </section>
  )
}

function toMetadata(detail: DocumentDetail): DocumentMetadata {
  const d = detail.document
  return { title: d.title, sourceId: d.sourceId, url: d.url, license: d.license, language: d.language, accessedAt: d.accessedAt }
}
