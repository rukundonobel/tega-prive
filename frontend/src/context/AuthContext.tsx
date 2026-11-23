import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { authService, type RegisterPayload } from '@/services/auth.service'
import type { AuthTokens, Role, User } from '@/types'

const ACCESS_TOKEN_KEY = 'tp_access_token'
const REFRESH_TOKEN_KEY = 'tp_refresh_token'

interface AuthContextValue {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (payload: RegisterPayload) => Promise<User>
  refresh: () => Promise<User>
  logout: () => void
  hasRole: (role: Role) => boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function storeTokens(tokens: AuthTokens): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  // Hydrate the current user if a token is already present.
  useEffect(() => {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (!token) {
      setLoading(false)
      return
    }
    authService
      .me()
      .then((u) => setUser(u))
      .catch(() => clearTokens())
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email: string, password: string): Promise<User> => {
    const tokens = await authService.login(email, password)
    storeTokens(tokens)
    setUser(tokens.user)
    return tokens.user
  }, [])

  const register = useCallback(async (payload: RegisterPayload): Promise<User> => {
    const tokens = await authService.register(payload)
    storeTokens(tokens)
    setUser(tokens.user)
    return tokens.user
  }, [])

  const refresh = useCallback(async (): Promise<User> => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!refreshToken) throw new Error('No refresh token available')
    const tokens = await authService.refresh(refreshToken)
    storeTokens(tokens)
    setUser(tokens.user)
    return tokens.user
  }, [])

  const logout = useCallback(() => {
    clearTokens()
    setUser(null)
  }, [])

  const hasRole = useCallback((role: Role): boolean => !!user && user.roles.includes(role), [user])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, refresh, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
