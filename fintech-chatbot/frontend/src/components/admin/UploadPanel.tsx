import { useState } from 'react'
import { Loader2, X } from 'lucide-react'
import { ApiError } from '../../api/client'
import { knowledgeApi, type DocumentDetail, type DocumentMetadata } from '../../api/knowledge'
import { todayIso } from '../../lib/format'
import MetadataForm, { emptyMetadata } from './MetadataForm'

type Mode = 'file' | 'text' | 'url'

interface Props {
  onCreated: (detail: DocumentDetail) => void
  onClose: () => void
}

const ACCEPT = '.pdf,.docx,.txt,.md,.html,.htm'
const MODES: { id: Mode; label: string }[] = [
  { id: 'file', label: 'Tải file' },
  { id: 'text', label: 'Dán văn bản' },
  { id: 'url', label: 'Từ URL' },
]

/**
 * UC-C01 bước 2–5 — Thêm tài liệu: tải file (PDF/DOCX/TXT/MD/HTML), dán văn bản, hoặc nhập từ URL trang công khai.
 * Tài liệu mới ở trạng thái "Chờ duyệt" để Admin xem trước các đoạn rồi mới duyệt.
 */
export default function UploadPanel({ onCreated, onClose }: Props) {
  const [mode, setMode] = useState<Mode>('file')
  const [metadata, setMetadata] = useState<DocumentMetadata>({ ...emptyMetadata, accessedAt: todayIso() })
  const [file, setFile] = useState<File | null>(null)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function submit() {
    setError(null)
    setFieldErrors({})
    if (mode === 'file' && !file) return setError('Hãy chọn file.')
    if (mode === 'text' && text.trim().length < 20) return setError('Văn bản quá ngắn.')
    if (mode === 'url' && !metadata.url) return setError('Hãy nhập URL trang công khai.')
    setBusy(true)
    try {
      let detail: DocumentDetail
      if (mode === 'file') {
        detail = await knowledgeApi.upload(file!, file!.name, metadata)
      } else if (mode === 'text') {
        // Dán văn bản: gửi như file Markdown, dòng bắt đầu bằng # là đề mục
        detail = await knowledgeApi.upload(new Blob([text], { type: 'text/markdown' }), 'van-ban.md', metadata)
      } else {
        detail = await knowledgeApi.importUrl(metadata)
      }
      onCreated(detail)
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message)
        setFieldErrors(e.details ?? {})
      } else {
        setError('Không tải lên được tài liệu.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="enter rounded-lg border border-stone-200 bg-white">
      <header className="flex items-center justify-between border-b border-stone-200 px-5 py-3">
        <h2 className="font-semibold tracking-[-0.01em] text-ink">Thêm tài liệu</h2>
        <button onClick={onClose} className="btn btn-ghost btn-icon btn-sm" title="Đóng" aria-label="Đóng">
          <X size={16} />
        </button>
      </header>

      <div className="space-y-5 p-5">
        <div className="inline-flex rounded-lg bg-stone-100 p-0.5 text-sm" role="tablist">
          {MODES.map((m) => (
            <button
              key={m.id}
              role="tab"
              aria-selected={mode === m.id}
              onClick={() => setMode(m.id)}
              className={`rounded-md px-3 py-1.5 transition-colors ${mode === m.id ? 'bg-white font-medium text-ink shadow-sm' : 'text-stone-500 hover:text-ink'}`}
            >
              {m.label}
            </button>
          ))}
        </div>

        {mode === 'file' && (
          <label className="flex cursor-pointer items-center justify-between gap-4 rounded-lg border border-dashed border-stone-300 px-4 py-4 text-sm hover:border-stone-400">
            <span className={file ? 'truncate text-ink' : 'text-stone-500'}>{file ? file.name : 'PDF, DOCX, TXT, MD hoặc HTML'}</span>
            <span className="btn btn-secondary btn-sm shrink-0">Chọn file</span>
            <input
              type="file"
              accept={ACCEPT}
              className="sr-only"
              onChange={(e) => {
                const f = e.target.files?.[0] ?? null
                setFile(f)
                if (f && !metadata.title) setMetadata((m) => ({ ...m, title: f.name.replace(/\.[^.]+$/, '') }))
              }}
            />
          </label>
        )}
        {mode === 'text' && (
          <div>
            <textarea
              className="input h-48 font-mono text-[13px]"
              placeholder={'# Đề mục\nNội dung đoạn văn…\n\n# Đề mục khác\n…'}
              value={text}
              onChange={(e) => setText(e.target.value)}
              aria-label="Nội dung tài liệu"
            />
          </div>
        )}

        <MetadataForm value={metadata} onChange={setMetadata} errors={fieldErrors} urlLabel={mode === 'url' ? 'URL trang hoặc file công khai' : undefined} />

        {error && <p className="text-sm text-negative">{error}</p>}
      </div>

      <footer className="flex justify-end gap-2 border-t border-stone-200 bg-stone-50 px-5 py-3">
        <button onClick={onClose} className="btn btn-ghost">
          Hủy
        </button>
        <button onClick={() => void submit()} disabled={busy} className="btn btn-primary">
          {busy && <Loader2 size={14} className="animate-spin" />}
          Tải lên và chia đoạn
        </button>
      </footer>
    </section>
  )
}
