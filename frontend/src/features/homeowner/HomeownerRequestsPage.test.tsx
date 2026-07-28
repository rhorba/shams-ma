import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import HomeownerRequestsPage from './HomeownerRequestsPage'
import { renderWithProviders } from '../../test-utils'

const QUOTED = [
  {
    id: 'qr-1',
    installerId: 'inst-1',
    installerBusinessName: 'Solaire Atlas',
    status: 'QUOTED',
    quoteAmount: 50000,
    quoteNotes: '5kWp system',
    createdAt: '2026-07-28T00:00:00Z',
  },
]

describe('HomeownerRequestsPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists quote requests', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify(QUOTED), { status: 200 }))

    renderWithProviders(<HomeownerRequestsPage />)

    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())
    expect(screen.getByText('QUOTED')).toBeInTheDocument()
  })

  it('shows an empty state when there are no requests', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<HomeownerRequestsPage />)

    await waitFor(() => expect(screen.getByText(/haven't requested/i)).toBeInTheDocument())
  })

  it('books a quoted request, refetches, and navigates to the checkout page', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(QUOTED), { status: 200 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ id: 'booking-1', status: 'PENDING_PAYMENT' }), {
          status: 201,
        }),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(
      <Routes>
        <Route path="/homeowner/requests" element={<HomeownerRequestsPage />} />
        <Route path="/homeowner/bookings/:bookingId/checkout" element={<div>checkout page</div>} />
      </Routes>,
      ['/homeowner/requests'],
    )
    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /^book$/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/homeowner/quote-requests/qr-1/book'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
    expect(await screen.findByText('checkout page')).toBeInTheDocument()
  })
})
