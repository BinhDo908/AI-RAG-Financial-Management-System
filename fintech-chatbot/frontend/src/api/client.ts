import type { ApiResponse } from './types'

const TOKEN_KEY = 'fintech.token'

/** Lỗi từ backend: mã lỗi (ErrorCode) + thông điệp tiếng Việt + lỗi theo trường (validation). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly details: Record<string, string> | null

  constructor(status: number, code: string, message: string, details: Record<string, string> | null = null) {
    super(message)
    this.status = status
    this.code = code
    this.details = details
  }
}

export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      // Trình duyệt chặn localStorage: phiên chỉ sống tới khi tải lại trang
    }
  },
}

let onUnauthorized: (() => void) | null = null

/** AuthContext đăng ký hàm này để tự đăng xuất khi token hết hạn / bị thu hồi. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

/** Dùng cho request tự gọi fetch (stream, tải file): token hết hạn → đăng xuất như các request khác. */
export function notifyUnauthorized() {
  onUnauthorized?.()
}

type Body =Record<string, unknown> | unknown[] | FormData | undefined

async function request<T>(method: string, path: string, body?: Body): Promise<T> {
  const headers: Record<string, string> = {}
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  let payload: BodyInit | undefined
  if (body instanceof FormData) {
    payload = body
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }

  let res: Response
  try {
    res = await fetch(path, { method, headers, body: payload })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Không kết nối được máy chủ. Kiểm tra mạng hoặc backend đã chạy chưa.')
  }

  let json: ApiResponse<T> | null = null
  const text = await res.text()
  if (text) {
    try {
      json = JSON.parse(text) as ApiResponse<T>
    } catch {
      json = null
    }
  }

  if (!res.ok || !json || json.success === false) {
    if (res.status === 401 && token) onUnauthorized?.()
    throw new ApiError(
      res.status,
      json?.errorCode ?? 'HTTP_' + res.status,
      json?.message ?? `Lỗi máy chủ (HTTP ${res.status})`,
      json?.details ?? null,
    )
  }
  return json.data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: Body) => request<T>('POST', path, body),
  put: <T>(path: string, body?: Body) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: Body) => request<T>('PATCH', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
}

/** Dựng query string, bỏ qua giá trị rỗng. */
export function qs(params: Record<string, string | number | boolean | null | undefined>): string {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') sp.set(k, String(v))
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}
