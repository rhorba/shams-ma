import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { useEffect, useState } from 'react'
import { Route, Routes } from 'react-router-dom'
import RequireRole from './RequireRole'
import { useAuth } from './AuthContext'
import { renderWithProviders } from '../test-utils'

/**
 * Logs in on mount and only renders the protected tree once authenticated. RequireRole redirects
 * to /login on its very first render if there's no role yet, and that redirect is permanent within
 * the same router — so the protected route must not mount until login has actually resolved.
 */
function LoggedInThen({ email, password, children }: { email: string; password: string; children: React.ReactNode }) {
  const { login, role } = useAuth()
  const [ready, setReady] = useState(false)
  useEffect(() => {
    login(email, password).then(() => setReady(true))
    // Intentionally run once on mount — this is test-only login-seeding, not app code.
  }, [])
  if (!ready || !role) return null
  return <>{children}</>
}

function Protected() {
  return (
    <Routes>
      <Route path="/login" element={<div>login page</div>} />
      <Route
        path="/installer"
        element={
          <RequireRole role="INSTALLER">
            <div>installer dashboard</div>
          </RequireRole>
        }
      />
      <Route
        path="/admin"
        element={
          <RequireRole role="ADMIN" requireMfa>
            <div>admin dashboard</div>
          </RequireRole>
        }
      />
    </Routes>
  )
}

describe('RequireRole', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('redirects to /login when not authenticated', () => {
    renderWithProviders(<Protected />, ['/installer'])
    expect(screen.getByText('login page')).toBeInTheDocument()
  })

  it('blocks access when the authenticated role does not match', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900, role: 'HOMEOWNER', mfaEnrolled: false }),
        { status: 200 },
      ),
    )

    renderWithProviders(
      <LoggedInThen email="a@example.com" password="pw">
        <Protected />
      </LoggedInThen>,
      ['/installer'],
    )

    await waitFor(() => expect(screen.getByText(/don't have access/i)).toBeInTheDocument(), {
      timeout: 3000,
    })
  })

  it('blocks access when MFA is required but not enrolled', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900, role: 'ADMIN', mfaEnrolled: false }),
        { status: 200 },
      ),
    )

    renderWithProviders(
      <LoggedInThen email="admin@example.com" password="pw">
        <Protected />
      </LoggedInThen>,
      ['/admin'],
    )

    await waitFor(() => expect(screen.getByText(/mfa enrollment is required/i)).toBeInTheDocument(), {
      timeout: 3000,
    })
  })
})
