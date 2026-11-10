import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { ApiError } from '../api/client'
import { chatApi, type ReplyStage } from '../api/chat'
import { prepareReceiptImage } from '../lib/image'
import type { Category, ChatMessage, Conversation, InputMode, TransactionRequest, Wallet } from '../api/types'
import ConversationSidebar from '../components/chat/ConversationSidebar'
import MessageBubble from '../components/chat/MessageBubble'
import Composer from '../components/chat/Composer'
import Mascot, { type MascotMood } from '../components/mascot/Mascot'

const CAPABILITIES = [
  { label: 'Ghi chép', example: 'Trưa nay ăn phở 50k' },
  { label: 'Số liệu của bạn', example: 'Tháng này tôi chi bao nhiêu cho ăn uống?' },
  { label: 'Kiến thức', example: 'Quỹ khẩn cấp nên để bao nhiêu tiền?' },
  { label: 'Tư vấn', example: 'Tôi nên tiết kiệm thế nào?' },
  { label: 'Tỷ giá', example: '100 đô bằng bao nhiêu tiền Việt?' },
]

/** Khung chat: mọi use case AI (A11–A13, B01–B03, B07, B09) và lịch sử hội thoại (B08). */
export default function ChatPage() {
  const params = useParams()
  const navigate = useNavigate()
  const activeId = params.conversationId ? Number(params.conversationId) : null

  const [conversations, setConversations] = useState<Conversation[]>([])
  const [keyword, setKeyword] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [pending, setPending] = useState<ChatMessage | null>(null)
  /** Câu trả lời đang được stream: bước hiện tại + phần chữ đã nhận. */
  const [reply, setReply] = useState<{ stage: ReplyStage | 'READING'; text: string } | null>(null)
  /** Vừa trả lời xong: mascot giữ trạng thái "xong" tới khi người dùng gõ lại hoặc đổi hội thoại. */
  const [justAnswered, setJustAnswered] = useState(false)
  const abortRef = useRef<AbortController | null>(null)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [wallets, setWallets] = useState<Wallet[]>([])
  const [prefill, setPrefill] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  /** Hội thoại vừa được tạo bởi tin nhắn đầu tiên: đã có tin nhắn trong state, không cần tải lại. */
  const justCreatedRef = useRef<number | null>(null)

  const loadConversations = useCallback(() => {
    chatApi
      .conversations(keyword)
      .then((page) => setConversations(page.items))
      .catch(() => undefined)
  }, [keyword])

  useEffect(() => {
    const t = window.setTimeout(loadConversations, 250)
    return () => window.clearTimeout(t)
  }, [loadConversations])

  const loadLookups = useCallback(() => {
    chatApi.categories().then(setCategories).catch(() => undefined)
    chatApi.wallets().then((w) => setWallets(w.wallets)).catch(() => undefined)
  }, [])

  useEffect(loadLookups, [loadLookups])

  useEffect(() => {
    setError(null)
    if (activeId === null) {
      setJustAnswered(false)
      setMessages([])
      return
    }
    if (justCreatedRef.current === activeId) {
      justCreatedRef.current = null
      return
    }
    setJustAnswered(false)
    chatApi
      .conversation(activeId)
      .then((c) => setMessages(c.messages))
      .catch((e: unknown) => {
        setError(e instanceof ApiError ? e.message : 'Không mở được hội thoại')
        setMessages([])
      })
  }, [activeId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, pending, reply?.text])

  // Rời trang khi đang chờ: ngắt stream (server vẫn lưu câu trả lời vào hội thoại)
  useEffect(() => () => abortRef.current?.abort(), [])

  async function send(text: string, mode: InputMode) {
    setError(null)
    setSending(true)
    setJustAnswered(false)
    setPending({ id: -1, role: 'USER', content: text, inputMode: mode, createdAt: new Date().toISOString() })
    setReply({ stage: 'THINKING', text: '' })
    const controller = new AbortController()
    abortRef.current = controller
    try {
      const res = await chatApi.sendStream(text, activeId, mode, {
        signal: controller.signal,
        onStage: (stage) => setReply((r) => (r ? { ...r, stage } : r)),
        onDelta: (delta) => setReply((r) => (r ? { ...r, text: r.text + delta } : r)),
      })
      setMessages((prev) => [...prev, res.userMessage, res.reply])
      setJustAnswered(true)
      if (activeId !== res.conversationId) {
        justCreatedRef.current = res.conversationId
        navigate(`/chat/${res.conversationId}`)
      }
      loadConversations()
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') return
      setError(
        e instanceof ApiError && e.code === 'AI_NOT_CONFIGURED'
          ? 'Chức năng AI chưa được cấu hình trên máy chủ (thiếu API key hoặc model).'
          : e instanceof Error
            ? e.message
            : 'Không gửi được tin nhắn',
      )
    } finally {
      setPending(null)
      setReply(null)
      setSending(false)
    }
  }

  /** UC-A14: ảnh hóa đơn → thẻ nháp; ảnh không được lưu, tin nhắn chỉ ghi "Ảnh hóa đơn". */
  async function sendReceipt(file: File) {
    setError(null)
    setSending(true)
    setJustAnswered(false)
    setPending({ id: -1, role: 'USER', content: 'Ảnh hóa đơn', inputMode: 'IMAGE', createdAt: new Date().toISOString() })
    setReply({ stage: 'READING', text: '' })
    try {
      const res = await chatApi.receipt(await prepareReceiptImage(file), activeId)
      setMessages((prev) => [...prev, res.userMessage, res.reply])
      setJustAnswered(true)
      if (activeId !== res.conversationId) {
        justCreatedRef.current = res.conversationId
        navigate(`/chat/${res.conversationId}`)
      }
      loadConversations()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không gửi được ảnh')
    } finally {
      setPending(null)
      setReply(null)
      setSending(false)
    }
  }

  async function saveDrafts(transactions: TransactionRequest[], mode: InputMode) {
    const res = await chatApi.confirmTransactions(activeId, mode, transactions)
    if (res.reply) setMessages((prev) => [...prev, res.reply!])
    loadLookups()
  }

  async function rename(id: number, title: string) {
    await chatApi.renameConversation(id, title)
    loadConversations()
  }

  async function remove(id: number) {
    await chatApi.deleteConversation(id)
    if (id === activeId) navigate('/chat')
    loadConversations()
  }

  const onPrefillConsumed = useCallback(() => setPrefill(null), [])
  const onTyping = useCallback(() => setJustAnswered(false), [])

  const mood: MascotMood = reply ? (reply.text ? 'writing' : 'thinking') : justAnswered ? 'done' : 'idle'
  const isEmpty = messages.length === 0 && !pending

  return (
    <div className="flex h-full bg-white">
      <div className="hidden md:block">
        <ConversationSidebar
          conversations={conversations}
          activeId={activeId}
          keyword={keyword}
          onKeywordChange={setKeyword}
          onSelect={(id) => navigate(`/chat/${id}`)}
          onNew={() => navigate('/chat')}
          onRename={(id, title) => void rename(id, title)}
          onDelete={(id) => void remove(id)}
        />
      </div>

      <section className="flex min-w-0 flex-1 flex-col">
        <div className="min-h-0 flex-1 overflow-y-auto">
          <div className="mx-auto max-w-2xl space-y-6 px-4 py-8 text-[15px]">
            {isEmpty && <EmptyState mood={mood} onPick={(s) => void send(s, 'TEXT')} disabled={sending} />}
            {messages.map((m, i) => (
              <MessageBubble
                key={m.id}
                message={m}
                categories={categories}
                wallets={wallets}
                draftsSaved={messages[i + 1]?.payload?.type === 'TRANSACTIONS_SAVED'}
                onSaveDrafts={(tx) => saveDrafts(tx, messages[i - 1]?.inputMode ?? 'TEXT')}
                onFeedback={async (rating, reason, comment) => {
                  await chatApi.feedback(m.id, rating, reason, comment)
                }}
                onClarify={setPrefill}
              />
            ))}
            {pending && (
              <>
                <MessageBubble
                  message={pending}
                  categories={[]}
                  wallets={[]}
                  draftsSaved={false}
                  onSaveDrafts={async () => undefined}
                  onFeedback={async () => undefined}
                  onClarify={() => undefined}
                />
                <StreamingReply stage={reply?.stage ?? 'THINKING'} text={reply?.text ?? ''} />
              </>
            )}
            {error && <p className="enter border-l-2 border-negative pl-3 text-sm text-negative">{error}</p>}
            <div ref={bottomRef} />
          </div>
        </div>
        <div className="mx-auto w-full max-w-2xl">
          {!isEmpty && (
            <div className="-mb-1 flex px-4 pt-1">
              <Mascot mood={mood} size="sm" />
            </div>
          )}
          <Composer
            disabled={sending}
            prefill={prefill}
            onPrefillConsumed={onPrefillConsumed}
            onTyping={onTyping}
            onSend={(t, m) => void send(t, m)}
            onImage={(f) => void sendReceipt(f)}
          />
        </div>
      </section>
    </div>
  )
}

/** Trạng thái trống: nói rõ trợ lý làm được 4 việc gì, mỗi việc kèm một câu mẫu bấm là gửi. */
function EmptyState({ mood, onPick, disabled }: { mood: MascotMood; onPick: (s: string) => void; disabled: boolean }) {
  return (
    <div className="pt-2 sm:pt-6">
      <Mascot mood={mood} />
      <h1 className="mt-4 text-center text-2xl font-semibold tracking-[-0.02em] text-ink">Bạn cần gì hôm nay?</h1>
      <ul className="mt-8 divide-y divide-stone-200 border-y border-stone-200">
        {CAPABILITIES.map((c) => (
          <li key={c.example}>
            <button
              disabled={disabled}
              onClick={() => onPick(c.example)}
              className="group flex w-full items-baseline gap-4 py-3.5 text-left disabled:opacity-50"
            >
              <span className="hidden w-28 shrink-0 text-sm text-stone-500 sm:block">{c.label}</span>
              <span className="min-w-0 flex-1 text-ink group-hover:underline group-hover:decoration-stone-300 group-hover:underline-offset-4">
                {c.example}
              </span>
              <ArrowRight size={15} className="shrink-0 self-center text-stone-300 transition-colors group-hover:text-ink" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

const STAGE_LABELS: Record<ReplyStage | 'READING', string> = {
  READING: 'Đang đọc hóa đơn',
  THINKING: 'Đang hiểu câu hỏi',
  SEARCHING: 'Đang tìm trong tài liệu',
  WRITING: 'Đang soạn câu trả lời',
}

/** Câu trả lời đang về: trước chữ đầu tiên thì hiện bước đang làm, sau đó hiện dần nội dung. */
function StreamingReply({ stage, text }: { stage: ReplyStage | 'READING'; text: string }) {
  if (!text) {
    return (
      <div className="enter flex items-center gap-2 text-sm text-stone-400" role="status" aria-live="polite">
        <span className="flex gap-1" aria-hidden>
          <span className="size-1.5 animate-pulse rounded-full bg-stone-400" />
          <span className="size-1.5 animate-pulse rounded-full bg-stone-400 [animation-delay:150ms]" />
          <span className="size-1.5 animate-pulse rounded-full bg-stone-400 [animation-delay:300ms]" />
        </span>
        {STAGE_LABELS[stage]}
      </div>
    )
  }
  return (
    <div className="prose-chat streaming-caret" aria-live="polite" aria-busy="true">
      <Markdown remarkPlugins={[remarkGfm]}>{text}</Markdown>
    </div>
  )
}
