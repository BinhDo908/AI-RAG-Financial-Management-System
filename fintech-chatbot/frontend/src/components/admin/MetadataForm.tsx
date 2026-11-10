import type { ReactNode } from 'react'
import { OPEN_SOURCES, type DocumentMetadata } from '../../api/knowledge'

interface Props {
  value: DocumentMetadata
  onChange: (value: DocumentMetadata) => void
  errors?: Record<string, string>
  urlLabel?: string
}

/** UC-C01 bước 3 — Metadata bắt buộc: tiêu đề, nguồn mở (SRC-xx), giấy phép, ngôn ngữ (BR-03, NFR-09). */
export default function MetadataForm({ value, onChange, errors = {}, urlLabel = 'URL gốc' }: Props) {
  const set = (patch: Partial<DocumentMetadata>) => onChange({ ...value, ...patch })

  function pickSource(id: string) {
    const src = OPEN_SOURCES.find((s) => s.id === id)
    // Điền sẵn giấy phép và ngôn ngữ gợi ý của nguồn; Admin vẫn sửa được
    set(src ? { sourceId: id, license: src.license, language: src.language } : { sourceId: id })
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <Field label="Tiêu đề" error={errors.title} className="sm:col-span-2">
        <input className="input" value={value.title} maxLength={300} onChange={(e) => set({ title: e.target.value })} />
      </Field>
      <Field label="Nguồn mở" error={errors.sourceId}>
        <select className="input" value={value.sourceId} onChange={(e) => pickSource(e.target.value)}>
          <option value="">Chọn nguồn</option>
          {OPEN_SOURCES.map((s) => (
            <option key={s.id} value={s.id}>
              {s.id} · {s.name}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Giấy phép" error={errors.license}>
        <input className="input" value={value.license} maxLength={200} onChange={(e) => set({ license: e.target.value })} />
      </Field>
      <Field label={urlLabel} error={errors.url} className="sm:col-span-2">
        <input className="input" value={value.url ?? ''} maxLength={1000} placeholder="https://" onChange={(e) => set({ url: e.target.value || null })} />
      </Field>
      <Field label="Ngôn ngữ" error={errors.language}>
        <select className="input" value={value.language} onChange={(e) => set({ language: e.target.value as 'vi' | 'en' })}>
          <option value="vi">Tiếng Việt</option>
          <option value="en">Tiếng Anh</option>
        </select>
      </Field>
      <Field label="Ngày truy cập" error={errors.accessedAt}>
        <input className="input" type="date" value={value.accessedAt ?? ''} onChange={(e) => set({ accessedAt: e.target.value || null })} />
      </Field>
    </div>
  )
}

export const emptyMetadata: DocumentMetadata = {
  title: '',
  sourceId: '',
  url: null,
  license: '',
  language: 'vi',
  accessedAt: null,
}

function Field({ label, error, className = '', children }: { label: string; error?: string; className?: string; children: ReactNode }) {
  return (
    <label className={`block ${className}`}>
      <span className="label">{label}</span>
      {children}
      {error && <span className="mt-1 block text-xs text-negative">{error}</span>}
    </label>
  )
}
