import { useEffect, useRef, type ReactNode } from 'react'
import { X } from 'lucide-react'

interface Props {
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}

/**
 * Hộp thoại giữa màn hình (modal giữ transform-origin ở tâm). Lớp nền mờ để tập trung vào tác vụ;
 * Esc hoặc bấm nền để đóng; focus vào phần tử đầu tiên khi mở.
 */
export default function Dialog({ title, onClose, children, footer }: Props) {
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    const panel = panelRef.current
    ;(panel?.querySelector<HTMLElement>('input:not([type=date]), select, textarea') ?? panel?.querySelector<HTMLElement>('button:not([data-close])'))?.focus()
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div className="dialog-scrim absolute inset-0 bg-stone-900/30" onClick={onClose} aria-hidden />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        className="dialog-panel relative flex max-h-[90dvh] w-full flex-col rounded-t-xl border border-stone-200 bg-white shadow-xl sm:max-w-lg sm:rounded-xl"
      >
        <header className="flex items-start justify-between gap-4 px-5 pt-5">
          <h2 id="dialog-title" className="text-lg font-semibold tracking-[-0.01em] text-ink">
            {title}
          </h2>
          <button data-close onClick={onClose} className="btn btn-ghost btn-icon btn-sm -mt-1 -mr-2" aria-label="Đóng">
            <X size={16} />
          </button>
        </header>
        <div className="min-h-0 overflow-y-auto px-5 py-5">{children}</div>
        {footer && <footer className="flex justify-end gap-2 border-t border-stone-200 bg-stone-50 px-5 py-3 sm:rounded-b-xl">{footer}</footer>}
      </div>
    </div>
  )
}
