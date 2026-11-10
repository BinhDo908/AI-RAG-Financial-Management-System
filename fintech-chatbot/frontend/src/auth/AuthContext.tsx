import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { AuthResponse, User } from '../api/types'

interface AuthState {
  user: User | null
  /** true khi đang kiểm tra token đã lưu lúc mở trang */
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
}

export interface RegisterInput {
  fullName: string
  email: string
  password: string
  confirmPassword: string
  acceptTerms: boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const clear = useCallback(() => {
    tokenStore.set(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(clear)
    if (!tokenStore.get()) {
      setLoading(false)
      return () => setUnauthorizedHandler(null)
    }
    api
      .get<User>('/api/me')
      .then(setUser)
      .catch(clear)
      .finally(() => setLoading(false))
    return () => setUnauthorizedHandler(null)
  }, [clear])

  const accept = useCallback((res: AuthResponse) => {
    tokenStore.set(res.accessToken)
    setUser(res.user)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      login: async (email, password) => accept(await api.post<AuthResponse>('/api/auth/login', { email, password })),
      register: async (input) => accept(await api.post<AuthResponse>('/api/auth/register', { ...input })),
      logout: async () => {
        try {
          await api.post('/api/auth/logout')
        } finally {
          clear()
        }
      },
    }),
    [user, loading, accept, clear],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải nằm trong AuthProvider')
  return ctx
}
