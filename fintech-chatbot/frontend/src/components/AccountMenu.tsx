import { useEffect, useRef, useState } from 'react'
import { LogOut } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

/** Nút tài khoản (chữ cái đầu tên) → thẻ nhỏ: tên, email, Đăng xuất. */
export default function AccountMenu({ placement }: { placement: 'rail' | 'top' }) {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => !ref.current?.contains(e.target as Node) && setOpen(false)
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const initial = (user?.fullName?.trim().split(/\s+/).pop() ?? '?').charAt(0).toUpperCase()
  const panelPosition = placement === 'rail' ? 'bottom-0 left-full ml-2 origin-bottom-left' : 'top-full right-0 mt-2 origin-top-right'

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Tài khoản"
        className="grid size-9 place-items-center rounded-full border border-stone-200 bg-white text-sm font-semibold text-ink transition-transform duration-150 active:scale-95"
      >
        {initial}
      </button>
      {open && (
        <div role="menu" className={`enter absolute z-40 w-60 rounded-xl border border-stone-200 bg-white p-1.5 shadow-lg ${panelPosition}`}>
          <div className="px-3 py-2">
            <div className="truncate text-sm font-medium text-ink">{user?.fullName}</div>
            <div className="truncate text-xs text-stone-500">{user?.email}</div>
          </div>
          <div className="my-1 h-px bg-stone-100" />
          <button
            role="menuitem"
            onClick={() => void logout()}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-stone-700 hover:bg-stone-50"
          >
            <LogOut size={15} aria-hidden /> Đăng xuất
          </button>
        </div>
      )}
    </div>
  )
}
