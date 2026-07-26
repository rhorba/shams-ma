import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CertificationUploadForm from './CertificationUploadForm'
import { renderWithProviders } from '../../test-utils'

describe('CertificationUploadForm', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('uploads a file and shows the returned status', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify({ id: 'cert-1', status: 'PENDING', uploadedAt: '2026-07-23T00:00:00Z' }),
        { status: 201 },
      ),
    )

    renderWithProviders(<CertificationUploadForm />)
    const file = new File(['%PDF-1.4'], 'cert.pdf', { type: 'application/pdf' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    await userEvent.upload(input, file)

    await waitFor(() => expect(screen.getByText('PENDING')).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/installer/certifications'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('shows an error message when the upload is rejected', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Unsupported file type' }), { status: 400 }),
    )

    renderWithProviders(<CertificationUploadForm />)
    // Passes the input's accept="" filter (extension/declared type), but the backend rejects it —
    // magic-byte content sniffing, not the client-declared type, is authoritative server-side.
    const file = new File(['not really a pdf'], 'bad.pdf', { type: 'application/pdf' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    await userEvent.upload(input, file)

    await waitFor(() => expect(screen.getByText('Unsupported file type')).toBeInTheDocument(), {
      timeout: 3000,
    })
  })
})
