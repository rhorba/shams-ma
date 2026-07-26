import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CertificationQueuePage from './CertificationQueuePage'
import { renderWithProviders } from '../../test-utils'

const SAMPLE = [
  {
    id: 'cert-1',
    installerId: 'inst-1',
    businessName: 'Solaire Atlas',
    status: 'PENDING',
    viewUrl: 'https://signed/cert-1.pdf',
    uploadedAt: '2026-07-23T00:00:00Z',
  },
]

describe('CertificationQueuePage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists pending certifications', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE), { status: 200 }))

    renderWithProviders(<CertificationQueuePage />)

    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())
  })

  it('shows an empty state when there are no pending certifications', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<CertificationQueuePage />)

    await waitFor(() => expect(screen.getByText(/no pending certifications/i)).toBeInTheDocument())
  })

  it('approves a certification and refetches the queue', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(new Response(JSON.stringify(SAMPLE), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 200, headers: { 'content-length': '0' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<CertificationQueuePage />)
    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /approve/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/admin/certifications/cert-1/approve'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
  })
})
