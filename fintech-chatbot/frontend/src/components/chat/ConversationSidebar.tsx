import { useMemo, useState, type ReactNode } from 'react'
import { Pencil, Plus, Search, Trash2 } from 'lucide-react'
import type { Conversation } from '../../api/types'

interface Props {
  conversations: Conversation[]
  activeId: number | null
  keyword: string
  onKeywordChange: (value: string) => void
  onSelect: (id: number) => void
  onNew: () => void
  onRename: (id: number, title: string) => void
  onDelete: (id: number) => void
}

/** UC-B08 — Lịch sử hội thoại, nhóm theo ngày như các app chat quen thuộc; đổi tên, xóa khi rê chuột. */
export default function ConversationSidebar(props: Props) {
  const { conversations, activeId, keyword, onKeywordChange, onSelect, onNew, onRename, onDelete } = props
  const [editingId, setEditingId] = useState<number | null>(null)
  const [draftTitle, setDraftTitle] = useState('')
  const groups = useMemo(() => groupByDay(conversations), [conversations])

  function commitEdit() {
    if (editingId !== null && draftTitle.trim()) onRename(editingId, draftTitle.trim())
    setEditingId(null)
  }

  return (
    <aside className="flex h-full w-64 shrink-0 flex-col border-r border-stone-200 bg-stone-50">
      <div className="space-y-2 p-3">
        <button onClick={onNew} className="btn btn-secondary w-full justify-start">
          <Plus size={15} /> Hội thoại mới
        </button>
        <div className="relative">
          <Search size={14} className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-stone-400" />
          <input value={keyword} onChange={(e) => onKeywordChange(e.target.value)} placeholder="Tìm hội thoại" className="input h-8 pl-8" />
        </div>
      </div>

      <nav className="min-h-0 flex-1 overflow-y-auto px-2 pb-4">
        {conversations.length === 0 && (
          <p className="px-2 py-6 text-sm text-stone-400">{keyword ? 'Không có hội thoại phù hợp.' : 'Chưa có hội thoại nào.'}</p>
        )}
        {groups.map(([label, items]) => (
          <div key={label} className="mt-3 first:mt-1">
            <div className="px-2 pb-1 text-xs font-medium text-stone-400">{label}</div>
            <ul>
              {items.map((c) =>
                editingId === c.id ? (
                  <li key={c.id}>
                    <input
                      autoFocus
                      value={draftTitle}
                      onChange={(e) => setDraftTitle(e.target.value)}
                      onBlur={commitEdit}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') commitEdit()
                        if (e.key === 'Escape') setEditingId(null)
                      }}
                      maxLength={200}
                      className="input h-8"
                    />
                  </li>
                ) : (
                  <li key={c.id}>
                    <div
                      onClick={() => onSelect(c.id)}
                      className={`group flex h-8 cursor-pointer items-center rounded-md pr-1 pl-2 text-sm ${
                        c.id === activeId ? 'bg-stone-200/70 font-medium text-ink' : 'text-stone-600 hover:bg-stone-100 hover:text-ink'
                      }`}
                    >
                      <span className="min-w-0 flex-1 truncate" title={c.title}>
                        {c.title}
                      </span>
                      <span className="hidden shrink-0 items-center group-hover:flex [@media(hover:none)]:flex">
                        <IconButton
                          label="Đổi tên"
                          onClick={() => {
                            setEditingId(c.id)
                            setDraftTitle(c.title)
                          }}
                        >
                          <Pencil size={13} />
                        </IconButton>
                        <IconButton
                          label="Xóa"
                          onClick={() => {
                            if (window.confirm('Xóa hội thoại này? Giao dịch đã lưu từ hội thoại vẫn được giữ.')) onDelete(c.id)
                          }}
                        >
                          <Trash2 size={13} />
                        </IconButton>
                      </span>
                    </div>
                  </li>
                ),
              )}
            </ul>
          </div>
        ))}
      </nav>
    </aside>
  )
}

function IconButton({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <button
      title={label}
      aria-label={label}
      onClick={(e) => {
        e.stopPropagation()
        onClick()
      }}
      className="flex size-6 items-center justify-center rounded text-stone-400 hover:bg-white hover:text-ink"
    >
      {children}
    </button>
  )
}

function groupByDay(conversations: Conversation[]): [string, Conversation[]][] {
  const startOfToday = new Date()
  startOfToday.setHours(0, 0, 0, 0)
  const day = 86_400_000
  const groups = new Map<string, Conversation[]>()
  for (const c of conversations) {
    const t = new Date(c.updatedAt).getTime()
    const label =
      t >= startOfToday.getTime()
        ? 'Hôm nay'
        : t >= startOfToday.getTime() - day
          ? 'Hôm qua'
          : t >= startOfToday.getTime() - 7 * day
            ? '7 ngày qua'
            : t >= startOfToday.getTime() - 30 * day
              ? '30 ngày qua'
              : 'Cũ hơn'
    groups.set(label, [...(groups.get(label) ?? []), c])
  }
  return [...groups.entries()]
}
