import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AdminBookingsOverviewPage from './AdminBookingsOverviewPage'
import { renderWithProviders } from '../../test-utils'

const SAMPLE_PAGE = {
  content: [
    {
      bookingId: 'booking-1',
      bookingStatus: 'PENDING_PAYMENT',
      depositAmount: 3000,
      bookingCreatedAt: '2026-07-28T00:00:00Z',
      homeownerName: 'Yasmine Alaoui',
      installerBusinessName: 'Solaire Atlas',
      paymentStatus: 'FAILED',
      paymentAmount: 999999,
      cmiTransactionId: 'txn-1',
      openFlagId: 'flag-1',
      openFlagReason: 'AMOUNT_MISMATCH',
      openFlagExpectedAmount: 3000,
      openFlagActualAmount: 999999,
    },
  ],
  totalElements: 1,
}

const EMPTY_PAGE = { content: [], totalElements: 0 }

describe('AdminBookingsOverviewPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists bookings with an open review flag', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)

    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())
    expect(screen.getByText(/AMOUNT_MISMATCH/)).toBeInTheDocument()
  })

  it('shows an empty state when no bookings match the filters', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify(EMPTY_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)

    await waitFor(() => expect(screen.getByText(/no bookings match/i)).toBeInTheDocument())
  })

  it('refetches with a search term and the needs-review-only filter applied', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)
    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())

    fireEvent.change(screen.getByLabelText('Search'), { target: { value: 'Yasmine' } })
    await waitFor(() =>
      expect(fetch).toHaveBeenLastCalledWith(expect.stringContaining('search=Yasmine'), expect.anything()),
    )

    await userEvent.click(screen.getByRole('checkbox', { name: /needs review only/i }))
    await waitFor(() =>
      expect(fetch).toHaveBeenLastCalledWith(expect.stringContaining('needsReviewOnly=true'), expect.anything()),
    )
  })

  it('dismisses a flagged payment and refetches the list', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 200, headers: { 'content-length': '0' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify(EMPTY_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)
    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /dismiss/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/admin/payment-flags/flag-1/dismiss'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
  })

  it('exports the current filter set as a CSV download', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response('bookingId,bookingStatus\n', { status: 200 }))
    const createObjectURL = vi.fn().mockReturnValue('blob:mock')
    const revokeObjectURL = vi.fn()
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL })

    renderWithProviders(<AdminBookingsOverviewPage />)
    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /export csv/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenLastCalledWith(
        expect.stringContaining('/api/v1/admin/bookings/export'),
        expect.anything(),
      ),
    )
    expect(createObjectURL).toHaveBeenCalled()
  })

  it('changes the page size via pagination controls', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)
    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('combobox', { name: /rows per page/i }))
    await userEvent.click(await screen.findByRole('option', { name: '50' }))

    await waitFor(() => expect(fetch).toHaveBeenLastCalledWith(expect.stringContaining('size=50'), expect.anything()))
  })

  it('resolves a flagged payment and refetches the list', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE_PAGE), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 200, headers: { 'content-length': '0' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify(EMPTY_PAGE), { status: 200 }))

    renderWithProviders(<AdminBookingsOverviewPage />)
    await waitFor(() => expect(screen.getByText('Yasmine Alaoui')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /resolve/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/admin/payment-flags/flag-1/resolve'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
  })
})
