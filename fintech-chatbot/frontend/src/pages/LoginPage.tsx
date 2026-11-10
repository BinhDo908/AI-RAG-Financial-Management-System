import { useState, type FormEvent, type ReactNode } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

type Mode = 'login' | 'register'

/** UC-D01 Đăng ký, UC-D02 Đăng nhập. */
export default function LoginPage() {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [mode, setMode] = useState<Mode>('login')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [acceptTerms, setAcceptTerms] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to={from} replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await register({
          fullName,
          email,
          password,
          confirmPassword,
          acceptTerms,
        })
      }
      navigate(from, { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(err.details ?? {})
      } else {
        setError('Có lỗi xảy ra, vui lòng thử lại')
      }
    } finally {
      setSubmitting(false)
    }
  }

  function switchMode(next: Mode) {
    setMode(next)
    setError(null)
    setFieldErrors({})
  }

  return (
    <div className="flex min-h-full flex-col items-center px-6 py-8">
      <div className="my-auto w-full max-w-sm py-10">
        <p className="text-[15px] font-semibold tracking-[-0.01em] text-ink">Trợ lý tài chính</p>
        <h1 className="mt-8 text-[28px] leading-tight font-semibold tracking-[-0.02em] text-ink">
          {mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}
        </h1>

        <form onSubmit={onSubmit} className="mt-8 space-y-4" noValidate>
          {mode === 'register' && (
            <Field label="Họ tên" error={fieldErrors.fullName}>
              <input className="input" value={fullName} onChange={(e) => setFullName(e.target.value)} autoComplete="name" required />
            </Field>
          )}
          <Field label="Email" error={fieldErrors.email}>
            <input className="input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          </Field>
          <Field label="Mật khẩu" error={fieldErrors.password} hint={mode === 'register' ? 'Ít nhất 8 ký tự, có cả chữ và số.' : undefined}>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              required
            />
          </Field>
          {mode === 'register' && (
            <>
              <Field label="Nhập lại mật khẩu" error={fieldErrors.confirmPassword}>
                <input
                  className="input"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                />
              </Field>
              <label className="flex items-start gap-2.5 text-sm leading-snug text-stone-600">
                <input
                  type="checkbox"
                  className="mt-0.5 size-4 accent-ink"
                  checked={acceptTerms}
                  onChange={(e) => setAcceptTerms(e.target.checked)}
                />
                <span>
                  Tôi đồng ý điều khoản sử dụng và hiểu rằng tư vấn của trợ lý chỉ mang tính tham khảo.
                  {fieldErrors.acceptTerms && <span className="mt-1 block text-negative">{fieldErrors.acceptTerms}</span>}
                </span>
              </label>
            </>
          )}

          {error && <p className="text-sm text-negative">{error}</p>}

          <button type="submit" disabled={submitting} className="btn btn-primary h-10 w-full">
            {submitting ? 'Đang xử lý…' : mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản'}
          </button>
        </form>

        <p className="mt-6 text-sm text-stone-500">
          {mode === 'login' ? 'Chưa có tài khoản? ' : 'Đã có tài khoản? '}
          <button
            className="font-medium text-ink underline decoration-stone-300 underline-offset-4 hover:decoration-ink"
            onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? 'Tạo tài khoản' : 'Đăng nhập'}
          </button>
        </p>
      </div>
    </div>
  )
}

function Field({ label, error, hint, children }: { label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {error ? (
        <span className="mt-1.5 block text-xs text-negative">{error}</span>
      ) : (
        hint && <span className="mt-1.5 block text-xs text-stone-400">{hint}</span>
      )}
    </label>
  )
}
