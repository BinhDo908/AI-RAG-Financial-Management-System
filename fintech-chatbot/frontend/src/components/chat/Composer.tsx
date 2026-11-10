import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { ArrowUp, Camera, Mic, Square } from 'lucide-react'
import type { InputMode } from '../../api/types'
import { useSpeechRecognition } from '../../hooks/useSpeechRecognition'

interface Props {
  disabled: boolean
  /** Nội dung chèn sẵn (ví dụ khi bấm nút "Ghi một khoản thu / chi"). */
  prefill: string | null
  onPrefillConsumed: () => void
  /** Người dùng vừa sửa nội dung ô nhập (mascot về trạng thái chờ). */
  onTyping: () => void
  onSend: (text: string, mode: InputMode) => void
  /** UC-A14: chọn / chụp ảnh hóa đơn. */
  onImage: (file: File) => void
}

/**
 * Ô nhập tin nhắn. Micro (UC-A12 / B03): nói → transcript hiện trong ô để sửa → gửi, tin nhắn gắn inputMode = VOICE.
 * Gửi bằng Enter không có animation (thao tác lặp lại nhiều lần mỗi ngày).
 */
export default function Composer({ disabled, prefill, onPrefillConsumed, onTyping, onSend, onImage }: Props) {
  const [text, setText] = useState('')
  const [mode, setMode] = useState<InputMode>('TEXT')
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const speech = useSpeechRecognition('vi-VN')

  useEffect(() => {
    if (speech.listening || speech.transcript) {
      setText(speech.transcript)
      if (speech.transcript) setMode('VOICE')
      onTyping()
    }
  }, [speech.listening, speech.transcript, onTyping])

  useEffect(() => {
    if (prefill !== null) {
      setText(prefill)
      setMode('TEXT')
      onPrefillConsumed()
      textareaRef.current?.focus()
    }
  }, [prefill, onPrefillConsumed])

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 160) + 'px'
  }, [text])

  function submit() {
    const value = text.trim()
    if (!value || disabled) return
    if (speech.listening) speech.stop()
    onSend(value, mode)
    setText('')
    setMode('TEXT')
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      submit()
    }
  }

  const canSend = !disabled && text.trim().length > 0

  return (
    <div className="px-4 pt-2 pb-4">
      {speech.error && <p className="mb-2 text-xs text-negative">{speech.error}</p>}
      <div className="rounded-xl border border-stone-300 bg-white transition-[border-color,box-shadow] duration-150 focus-within:border-stone-400 focus-within:shadow-[0_0_0_3px_rgba(28,25,23,0.06)]">
        <textarea
          ref={textareaRef}
          rows={1}
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            onTyping()
          }}
          onKeyDown={onKeyDown}
          placeholder={speech.listening ? 'Đang nghe, nói xong bấm dừng…' : 'Ghi khoản chi hoặc đặt câu hỏi'}
          className="block max-h-40 w-full resize-none bg-transparent px-4 pt-3 pb-1 text-ink outline-none placeholder:text-stone-400"
          maxLength={2000}
          aria-label="Tin nhắn"
        />
        <div className="flex items-center justify-between px-2 pb-2">
          <span className="px-2 text-xs text-stone-400">{mode === 'VOICE' && text ? 'Nhập bằng giọng nói' : ''}</span>
          <div className="flex items-center gap-1">
            {/* Không đặt capture: điện thoại cho chọn chụp mới hoặc lấy ảnh có sẵn */}
            <input
              ref={fileRef}
              type="file"
              accept="image/jpeg,image/png,image/webp,image/*"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0]
                e.target.value = ''
                if (file) onImage(file)
              }}
            />
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              disabled={disabled}
              className="btn btn-ghost btn-icon btn-sm rounded-full"
              title="Quét hóa đơn"
              aria-label="Quét hóa đơn"
            >
              <Camera size={16} />
            </button>
            {speech.supported && (
              <button
                type="button"
                onClick={() => (speech.listening ? speech.stop() : speech.start())}
                disabled={disabled}
                className={`btn btn-icon btn-sm rounded-full ${speech.listening ? 'bg-negative text-white' : 'btn-ghost'}`}
                title={speech.listening ? 'Dừng ghi âm' : 'Nói thay vì gõ'}
                aria-label={speech.listening ? 'Dừng ghi âm' : 'Nói thay vì gõ'}
              >
                {speech.listening ? <Square size={12} fill="currentColor" /> : <Mic size={16} />}
              </button>
            )}
            <button
              type="button"
              onClick={submit}
              disabled={!canSend}
              className="btn btn-primary btn-icon btn-sm rounded-full"
              title="Gửi (Enter)"
              aria-label="Gửi"
            >
              <ArrowUp size={16} />
            </button>
          </div>
        </div>
      </div>
      <p className="mt-2 text-center text-[11px] text-stone-400">Thông tin tham khảo, không phải lời khuyên đầu tư.</p>
    </div>
  )
}
