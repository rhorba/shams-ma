import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import QuoteRequestInbox from './QuoteRequestInbox'
import { renderWithProviders } from '../../test-utils'

const REQUESTED = [
  {
    id: 'qr-1',
    homeownerId: 'home-1',
    homeownerFullName: 'Test Homeowner',
    status: 'REQUESTED',
    message: 'Interested in solar',
    quoteAmount: null,
    createdAt: '2026-07-28T00:00:00Z',
  },
]

describe('QuoteRequestInbox', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists incoming quote requests', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify(REQUESTED), { status: 200 }))

    renderWithProviders(<QuoteRequestInbox />)

    await waitFor(() => expect(screen.getByText('Test Homeowner')).toBeInTheDocument())
  })

  it('shows an empty state when there are no leads', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<QuoteRequestInbox />)

    await waitFor(() => expect(screen.getByText(/no quote requests/i)).toBeInTheDocument())
  })

  it('sends a quote amount and refetches the inbox', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(REQUESTED), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ...REQUESTED[0], status: 'QUOTED' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<QuoteRequestInbox />)
    await waitFor(() => expect(screen.getByText('Test Homeowner')).toBeInTheDocument())

    await userEvent.type(screen.getByLabelText(/amount/i), '50000')
    await userEvent.click(screen.getByRole('button', { name: /send quote/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/installer/quote-requests/qr-1/respond'),
        expect.objectContaining({ method: 'POST', body: JSON.stringify({ action: 'QUOTE', quoteAmount: 50000 }) }),
      ),
    )
  })

  it('declines a request and refetches the inbox', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(REQUESTED), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ...REQUESTED[0], status: 'DECLINED' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<QuoteRequestInbox />)
    await waitFor(() => expect(screen.getByText('Test Homeowner')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /decline/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/installer/quote-requests/qr-1/respond'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ action: 'DECLINE', quoteAmount: undefined }),
        }),
      ),
    )
  })
})
