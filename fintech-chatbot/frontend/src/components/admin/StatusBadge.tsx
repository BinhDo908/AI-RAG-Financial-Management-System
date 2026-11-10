import type { DocumentStatus } from '../../api/knowledge'

const STYLES: Record<DocumentStatus, { label: string; dot: string; text: string }> = {
  DRAFT: { label: 'Chờ duyệt', dot: 'bg-amber-500', text: 'text-stone-700' },
  APPROVED: { label: 'Đã duyệt', dot: 'bg-accent-500', text: 'text-stone-700' },
  DISABLED: { label: 'Đã tắt', dot: 'bg-stone-300', text: 'text-stone-400' },
}

/** Trạng thái dạng chấm + chữ: đọc nhanh trong bảng mà không thành một dãy "pill" màu. */
export default function StatusBadge({ status }: { status: DocumentStatus }) {
  const s = STYLES[status]
  return (
    <span className={`inline-flex items-center gap-1.5 text-sm whitespace-nowrap ${s.text}`}>
      <span className={`size-1.5 rounded-full ${s.dot}`} />
      {s.label}
    </span>
  )
}
