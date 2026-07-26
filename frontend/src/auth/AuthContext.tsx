import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { apiFetch } from '../lib/api'

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  role: 'HOMEOWNER' | 'INSTALLER' | 'ADMIN'
  mfaEnrolled: boolean
}

interface AuthState {
  accessToken: string | null
  role: AuthResponse['role'] | null
  mfaEnrolled: boolean
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

// Access token kept in memory only (not localStorage) per Security doc section 3 — reduces
// XSS token-theft risk. Refresh token is an HttpOnly cookie the browser manages automatically.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ accessToken: null, role: null, mfaEnrolled: false })

  const login = async (email: string, password: string) => {
    const response = await apiFetch<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
    setState({ accessToken: response.accessToken, role: response.role, mfaEnrolled: response.mfaEnrolled })
  }

  const logout = () => setState({ accessToken: null, role: null, mfaEnrolled: false })

  const value = useMemo(() => ({ ...state, login, logout }), [state])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
