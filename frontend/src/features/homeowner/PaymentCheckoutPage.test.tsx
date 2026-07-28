import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import PaymentCheckoutPage from './PaymentCheckoutPage'
import { renderWithProviders } from '../../test-utils'

function renderCheckoutPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/homeowner/bookings/:bookingId/checkout" element={<PaymentCheckoutPage />} />
    </Routes>,
    ['/homeowner/bookings/booking-1/checkout'],
  )
}

const PENDING_BOOKING = {
  id: 'booking-1',
  quoteRequestId: 'qr-1',
  status: 'PENDING_PAYMENT',
  depositAmount: 5000,
  createdAt: '2026-07-28T00:00:00Z',
}

const CHECKOUT_SESSION = {
  paymentId: 'pay-1',
  checkoutUrl: '/api/v1/mock-cmi/MOCK-1',
  cmiTransactionId: 'MOCK-1',
  amount: 5000,
  currency: 'MAD',
}

describe('PaymentCheckoutPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows a success message when the booking is already paid', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ ...PENDING_BOOKING, status: 'BOOKED' }), { status: 200 }),
    )

    renderCheckoutPage()

    await waitFor(() => expect(screen.getByText(/booking is confirmed/i)).toBeInTheDocument())
  })

  it('starts checkout and shows the deposit amount with simulate actions', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(PENDING_BOOKING), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(CHECKOUT_SESSION), { status: 201 }))

    renderCheckoutPage()
    await userEvent.click(await screen.findByRole('button', { name: /proceed to payment/i }))

    await waitFor(() => expect(screen.getByText(/5.000 MAD/)).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /simulate successful payment/i })).toBeInTheDocument()
  })

  it('confirms the booking after simulating a successful payment', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(PENDING_BOOKING), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(CHECKOUT_SESSION), { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 200, headers: { 'content-length': '0' } }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ ...PENDING_BOOKING, status: 'BOOKED' }), { status: 200 }),
      )

    renderCheckoutPage()
    await userEvent.click(await screen.findByRole('button', { name: /proceed to payment/i }))
    await userEvent.click(await screen.findByRole('button', { name: /simulate successful payment/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/mock-cmi/MOCK-1/succeed'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
    expect(await screen.findByText(/booking is confirmed/i)).toBeInTheDocument()
  })

  it('falls back to a fresh checkout after simulating a failed payment', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(PENDING_BOOKING), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(CHECKOUT_SESSION), { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 200, headers: { 'content-length': '0' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify(PENDING_BOOKING), { status: 200 }))

    renderCheckoutPage()
    await userEvent.click(await screen.findByRole('button', { name: /proceed to payment/i }))
    await userEvent.click(await screen.findByRole('button', { name: /simulate failed payment/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/mock-cmi/MOCK-1/fail'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
    expect(await screen.findByRole('button', { name: /proceed to payment/i })).toBeInTheDocument()
  })
})
