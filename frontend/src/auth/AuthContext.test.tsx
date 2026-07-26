import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'
import { AuthProvider, useAuth } from './AuthContext'

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('login stores the access token and role on success', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          accessToken: 'token-123',
          tokenType: 'Bearer',
          expiresIn: 900,
          role: 'INSTALLER',
          mfaEnrolled: false,
        }),
        { status: 200 },
      ),
    )

    const { result } = renderHook(() => useAuth(), { wrapper })

    await act(async () => {
      await result.current.login('a@example.com', 'password')
    })

    await waitFor(() => {
      expect(result.current.accessToken).toBe('token-123')
      expect(result.current.role).toBe('INSTALLER')
    })
  })

  it('login throws and leaves state unset on failure', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Invalid credentials' }), { status: 401 }),
    )

    const { result } = renderHook(() => useAuth(), { wrapper })

    await expect(
      act(async () => {
        await result.current.login('a@example.com', 'wrong')
      }),
    ).rejects.toThrow('Invalid credentials')

    expect(result.current.accessToken).toBeNull()
  })

  it('logout clears the auth state', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900, role: 'ADMIN', mfaEnrolled: true }),
        { status: 200 },
      ),
    )
    const { result } = renderHook(() => useAuth(), { wrapper })
    await act(async () => {
      await result.current.login('admin@example.com', 'password')
    })
    expect(result.current.role).toBe('ADMIN')

    act(() => {
      result.current.logout()
    })

    expect(result.current.role).toBeNull()
    expect(result.current.accessToken).toBeNull()
  })
})
