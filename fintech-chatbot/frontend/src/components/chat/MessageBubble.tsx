import { useState, type ReactNode } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { ArrowUpRight, Camera, ChevronRight, Mic, ThumbsDown, ThumbsUp } from 'lucide-react'
import type { Category, ChatMessage, FeedbackRating, FeedbackReason, TransactionRequest, Wallet } from '../../api/types'
import DraftCards from './DraftCards'

interface Props {
  message: ChatMessage
  categories: Category[]
  wallets: Wallet[]
  /** Thẻ giao dịch nháp của tin nhắn này đã được lưu (tin nhắn kế tiếp là TRANSACTIONS_SAVED). */
  draftsSaved: boolean
  onSaveDrafts: (transactions: TransactionRequest[]) => Promise<void>
  onFeedback: (rating: FeedbackRating, reason?: FeedbackReason, comment?: string) => Promise<void>
  onClarify: (prefix: string) => void
}

const INTENT_LABELS: Record<string, string> = {
  RECORD_TRANSACTION: 'Ghi giao dịch',
  QUERY_PERSONAL_DATA: 'Số liệu của bạn',
  KNOWLEDGE_QA: 'Kiến thức',
  PERSONAL_ADVICE: 'Tư vấn',
  MARKET_DATA: 'Tỷ giá',
}

export default function MessageBubble(props: Props) {
  const { message: m } = props
  if (m.role === 'USER') {
    return (
      <div className="enter flex justify-end">
        <div className="max-w-[85%] rounded-2xl bg-stone-200/70 px-4 py-2.5 whitespace-pre-wrap text-ink">
          {m.inputMode === 'VOICE' && <Mic size={13} className="mr-1.5 mb-0.5 inline text-stone-500" aria-label="Nhập bằng giọng nói" />}
          {m.inputMode === 'IMAGE' && <Camera size={14} className="mr-1.5 mb-0.5 inline text-stone-500" aria-hidden />}
          {m.content}
        </div>
      </div>
    )
  }
  return <AssistantMessage {...props} />
}

/** Câu trả lời không nằm trong khung: đọc như văn bản, công cụ (nguồn, đánh giá) xếp gọn bên dưới. */
function AssistantMessage({ message: m, categories, wallets, draftsSaved, onSaveDrafts, onFeedback, onClarify }: Props) {
  const payload = m.payload

  return (
    <div className="enter group">
      <div className="prose-chat">
        <Markdown remarkPlugins={[remarkGfm]}>{m.content}</Markdown>
      </div>

      {payload?.type === 'TRANSACTION_DRAFTS' && (
        <DraftCards drafts={payload.drafts} categories={categories} wallets={wallets} saved={draftsSaved} onSave={onSaveDrafts} />
      )}

      {payload?.type === 'CLARIFY' && (
        <div className="mt-3 flex flex-wrap gap-2">
          <button className="btn btn-secondary btn-sm" onClick={() => onClarify('Ghi giúp tôi: ')}>
            Ghi một khoản thu / chi
          </button>
          <button className="btn btn-secondary btn-sm" onClick={() => onClarify('Cho tôi hỏi: ')}>
            Hỏi thông tin
          </button>
        </div>
      )}

      {payload?.type === 'PERSONAL_DATA' && <DataDetails title="Số liệu hệ thống đã tính" data={payload.result} />}
      {payload?.type === 'FINANCIAL_SUMMARY' && <DataDetails title="Tóm tắt tài chính dùng để tư vấn" data={payload.summary} />}

      {m.citations && m.citations.length > 0 && (
        <ol className="mt-3 space-y-1 border-l-2 border-stone-200 pl-3 text-xs text-stone-500">
          {m.citations.map((c) => (
            <li key={`${c.chunkId}-${c.number}`} className="flex gap-1.5">
              <span className="tabular shrink-0 text-stone-400">[{c.number}]</span>
              <span className="min-w-0">
                <span className="text-stone-700">{c.title}</span>
                {c.heading && <> — {c.heading}</>}
                {c.pageNumber != null && <>, trang {c.pageNumber}</>}
                {c.url && (
                  <a
                    href={c.url}
                    target="_blank"
                    rel="noreferrer noopener"
                    className="ml-1 inline-flex items-center text-stone-500 underline decoration-stone-300 underline-offset-2 hover:text-ink"
                  >
                    nguồn <ArrowUpRight size={11} />
                  </a>
                )}
              </span>
            </li>
          ))}
        </ol>
      )}

      <MetaBar label={m.intent ? INTENT_LABELS[m.intent] : undefined} latencyMs={m.latencyMs} onFeedback={onFeedback} />
    </div>
  )
}

/**
 * Dòng phụ dưới câu trả lời: loại câu trả lời, thời gian, 👍 / 👎 (UC-B09).
 * Trên máy tính chỉ hiện rõ khi rê chuột vào câu trả lời; trên màn hình cảm ứng luôn hiện.
 */
function MetaBar({ label, latencyMs, onFeedback }: { label?: string; latencyMs?: number; onFeedback: Props['onFeedback'] }) {
  const [rating, setRating] = useState<FeedbackRating | null>(null)
  const [askingReason, setAskingReason] = useState(false)
  const [reason, setReason] = useState<FeedbackReason>('WRONG_INFO')
  const [comment, setComment] = useState('')

  async function up() {
    setRating('UP')
    setAskingReason(false)
    await onFeedback('UP')
  }

  async function submitDown() {
    setRating('DOWN')
    setAskingReason(false)
    await onFeedback('DOWN', reason, comment || undefined)
  }

  const visible = rating !== null || askingReason

  return (
    <div className="mt-2 text-xs text-stone-400">
      <div
        className={`flex items-center gap-1 transition-opacity duration-150 ${
          visible ? 'opacity-100' : 'opacity-0 group-hover:opacity-100 focus-within:opacity-100 [@media(hover:none)]:opacity-100'
        }`}
      >
        {label && <span>{label}</span>}
        {label && latencyMs != null && <span>·</span>}
        {latencyMs != null && <span className="tabular">{(latencyMs / 1000).toFixed(1)} giây</span>}
        <span className="ml-2 flex items-center">
          <FeedbackButton active={rating === 'UP'} label="Hữu ích" onClick={() => void up()}>
            <ThumbsUp size={13} />
          </FeedbackButton>
          <FeedbackButton active={rating === 'DOWN' || askingReason} label="Chưa đúng" onClick={() => setAskingReason(true)}>
            <ThumbsDown size={13} />
          </FeedbackButton>
        </span>
        {rating && <span className="text-stone-500">Đã ghi nhận góp ý</span>}
      </div>
      {askingReason && (
        <div className="enter mt-2 flex flex-wrap items-center gap-2">
          <select className="input h-8 w-auto" value={reason} onChange={(e) => setReason(e.target.value as FeedbackReason)}>
            <option value="WRONG_INFO">Sai thông tin</option>
            <option value="NO_SOURCE">Không có nguồn</option>
            <option value="IRRELEVANT">Không liên quan</option>
            <option value="WRONG_NUMBER">Số liệu sai</option>
            <option value="OTHER">Khác</option>
          </select>
          <input
            className="input h-8 min-w-40 flex-1"
            placeholder="Ghi chú thêm (không bắt buộc)"
            value={comment}
            maxLength={1000}
            onChange={(e) => setComment(e.target.value)}
          />
          <button className="btn btn-primary btn-sm" onClick={() => void submitDown()}>
            Gửi
          </button>
          <button className="btn btn-ghost btn-sm" onClick={() => setAskingReason(false)}>
            Hủy
          </button>
        </div>
      )}
    </div>
  )
}

function FeedbackButton({ active, label, onClick, children }: { active: boolean; label: string; onClick: () => void; children: ReactNode }) {
  return (
    <button
      title={label}
      aria-label={label}
      aria-pressed={active}
      onClick={onClick}
      className={`flex size-7 items-center justify-center rounded-md transition-colors hover:bg-stone-100 ${active ? 'text-ink' : 'hover:text-ink'}`}
    >
      {children}
    </button>
  )
}

/** Dữ liệu hệ thống đã tính (BR-06) để người dùng đối chiếu với câu trả lời. */
function DataDetails({ title, data }: { title: string; data: Record<string, unknown> }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="mt-3 text-xs">
      <button className="flex items-center gap-1 text-stone-500 hover:text-ink" onClick={() => setOpen(!open)} aria-expanded={open}>
        <ChevronRight size={13} className={`transition-transform duration-150 ${open ? 'rotate-90' : ''}`} /> {title}
      </button>
      {open && (
        <pre className="enter tabular mt-2 max-h-64 overflow-auto rounded-lg border border-stone-200 bg-white p-3 font-mono whitespace-pre-wrap text-stone-600">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  )
}
