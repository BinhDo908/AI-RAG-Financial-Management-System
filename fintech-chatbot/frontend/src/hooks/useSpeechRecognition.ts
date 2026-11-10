import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * UC-A12 / B03 — Nhận diện giọng nói bằng Web Speech API của trình duyệt (phương án 1 trong USE_CASES.md §9.3).
 * Âm thanh được xử lý bởi trình duyệt, ứng dụng không lưu audio (BR-09). Chrome / Edge hỗ trợ tốt; Firefox chưa hỗ trợ.
 * Tự dừng sau 60 giây (UC-A12 EX-3).
 */

interface SpeechRecognitionResultLike {
  readonly isFinal: boolean
  readonly 0: { transcript: string }
}
interface SpeechRecognitionEventLike {
  readonly resultIndex: number
  readonly results: ArrayLike<SpeechRecognitionResultLike>
}
interface SpeechRecognitionLike {
  lang: string
  continuous: boolean
  interimResults: boolean
  start(): void
  stop(): void
  abort(): void
  onresult: ((e: SpeechRecognitionEventLike) => void) | null
  onerror: ((e: { error: string }) => void) | null
  onend: (() => void) | null
}
type SpeechRecognitionCtor = new () => SpeechRecognitionLike

const MAX_DURATION_MS = 60_000

function getCtor(): SpeechRecognitionCtor | null {
  const w = window as unknown as { SpeechRecognition?: SpeechRecognitionCtor; webkitSpeechRecognition?: SpeechRecognitionCtor }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null
}

export function useSpeechRecognition(lang = 'vi-VN') {
  const supported = typeof window !== 'undefined' && getCtor() !== null
  const [listening, setListening] = useState(false)
  const [transcript, setTranscript] = useState('')
  const [error, setError] = useState<string | null>(null)
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null)
  const timerRef = useRef<number | null>(null)

  const stop = useCallback(() => {
    recognitionRef.current?.stop()
  }, [])

  const cancel = useCallback(() => {
    recognitionRef.current?.abort()
    setTranscript('')
  }, [])

  const start = useCallback(() => {
    const Ctor = getCtor()
    if (!Ctor) {
      setError('Trình duyệt chưa hỗ trợ nhận diện giọng nói, hãy dùng Chrome hoặc Edge.')
      return
    }
    setError(null)
    setTranscript('')
    const rec = new Ctor()
    rec.lang = lang
    rec.continuous = true
    rec.interimResults = true
    let finalText = ''
    rec.onresult = (e) => {
      let interim = ''
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i]
        if (r.isFinal) finalText += r[0].transcript
        else interim += r[0].transcript
      }
      setTranscript((finalText + interim).trim())
    }
    rec.onerror = (e) => {
      if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
        setError('Chưa có quyền dùng micro. Hãy cho phép micro trong cài đặt trình duyệt.')
      } else if (e.error === 'no-speech') {
        setError('Mình chưa nghe rõ, bạn thử lại nhé.')
      } else if (e.error !== 'aborted') {
        setError('Không nhận diện được giọng nói (' + e.error + ').')
      }
    }
    rec.onend = () => {
      setListening(false)
      if (timerRef.current) window.clearTimeout(timerRef.current)
      recognitionRef.current = null
    }
    recognitionRef.current = rec
    rec.start()
    setListening(true)
    timerRef.current = window.setTimeout(() => rec.stop(), MAX_DURATION_MS)
  }, [lang])

  useEffect(() => () => recognitionRef.current?.abort(), [])

  return { supported, listening, transcript, error, start, stop, cancel }
}
