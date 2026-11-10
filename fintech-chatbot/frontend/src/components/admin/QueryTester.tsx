import { useState, type FormEvent } from 'react'
import { Loader2 } from 'lucide-react'
import { ApiError } from '../../api/client'
import { knowledgeApi, type RetrievalTest } from '../../api/knowledge'

/**
 * UC-C01 AF-3 — Thử truy vấn: top_k đoạn truy xuất được, điểm tương đồng cosine, và đoạn nào đạt ngưỡng
 * (được đưa vào prompt). Dùng để chỉnh rag.score_threshold ở UC-C04.
 */
export default function QueryTester() {
  const [question, setQuestion] = useState('')
  const [result, setResult] = useState<RetrievalTest | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!question.trim()) return
    setBusy(true)
    setError(null)
    try {
      setResult(await knowledgeApi.testQuery(question.trim()))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không truy vấn được.')
    } finally {
      setBusy(false)
    }
  }

  const used = result?.hits.filter((h) => h.usedInPrompt).length ?? 0

  return (
    <section className="rounded-lg border border-stone-200 bg-white">
      <header className="border-b border-stone-200 px-5 py-3">
        <h2 className="font-semibold tracking-[-0.01em] text-ink">Thử truy vấn</h2>
      </header>
      <div className="p-5">
        <form onSubmit={submit} className="flex gap-2">
          <input className="input" value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="Quỹ khẩn cấp nên để bao nhiêu?" aria-label="Câu hỏi thử" />
          <button type="submit" disabled={busy || !question.trim()} className="btn btn-primary shrink-0">
            {busy && <Loader2 size={14} className="animate-spin" />}
            Tìm
          </button>
        </form>
        {error && <p className="mt-3 text-sm text-negative">{error}</p>}

        {result && (
          <div className="enter mt-4">
            <p className="text-sm text-stone-600">
              {result.sufficient ? (
                <>
                  <span className="font-medium text-ink">{used} đoạn</span> đạt ngưỡng{' '}
                  <span className="tabular">{result.threshold}</span> và được đưa vào câu trả lời.
                </>
              ) : (
                <>
                  Không đoạn nào đạt ngưỡng <span className="tabular">{result.threshold}</span> — trợ lý sẽ trả lời “không đủ thông tin”.
                </>
              )}
            </p>
            {result.hits.length === 0 ? (
              <p className="mt-3 text-sm text-stone-400">Kho tri thức chưa có tài liệu đã duyệt.</p>
            ) : (
              <ol className="mt-3 divide-y divide-stone-100 border-y border-stone-100">
                {result.hits.map((h) => (
                  <li key={h.chunkId} className="py-3">
                    <div className="flex items-baseline gap-3">
                      <span className={`tabular w-12 shrink-0 text-sm font-medium ${h.usedInPrompt ? 'text-accent-700' : 'text-stone-400'}`}>
                        {h.score.toFixed(3)}
                      </span>
                      <span className="min-w-0 flex-1 truncate text-sm text-ink" title={h.title}>
                        {h.heading ?? h.title}
                        {h.heading && <span className="text-stone-400"> · {h.title}</span>}
                      </span>
                      <span className={`shrink-0 text-xs ${h.usedInPrompt ? 'text-accent-700' : 'text-stone-400'}`}>
                        {h.usedInPrompt ? 'Dùng' : 'Dưới ngưỡng'}
                      </span>
                    </div>
                    <p className="mt-1 line-clamp-2 pl-15 text-sm text-stone-500">{h.content}</p>
                  </li>
                ))}
              </ol>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
