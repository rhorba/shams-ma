import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import LoginPage from './LoginPage'
import { renderWithProviders } from '../test-utils'

function LoginRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<div>home page</div>} />
    </Routes>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('navigates home after a successful login', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          accessToken: 't',
          tokenType: 'Bearer',
          expiresIn: 900,
          role: 'INSTALLER',
          mfaEnrolled: false,
        }),
        { status: 200 },
      ),
    )

    renderWithProviders(<LoginRoutes />, ['/login'])
    await userEvent.type(screen.getByLabelText(/email/i), 'a@example.com')
    await userEvent.type(screen.getByLabelText(/password/i), 'password')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => expect(screen.getByText('home page')).toBeInTheDocument())
  })

  it('shows an error message on failed login', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Invalid credentials' }), { status: 401 }),
    )

    renderWithProviders(<LoginRoutes />, ['/login'])
    await userEvent.type(screen.getByLabelText(/email/i), 'a@example.com')
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => expect(screen.getByText('Invalid credentials')).toBeInTheDocument())
  })
})
