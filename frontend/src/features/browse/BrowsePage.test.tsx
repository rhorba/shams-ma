import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BrowsePage from './BrowsePage'
import { renderWithProviders } from '../../test-utils'

describe('BrowsePage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('searches by lat/lng and renders results', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify([
          { userId: 'inst-1', businessName: 'Solaire Atlas', phone: '+212600000000', distanceKm: 4.2 },
        ]),
        { status: 200 },
      ),
    )

    renderWithProviders(<BrowsePage />)
    await userEvent.type(screen.getByLabelText(/latitude/i), '34.02')
    await userEvent.type(screen.getByLabelText(/longitude/i), '-6.84')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))

    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/installers/browse?lat=34.02&lng=-6.84'),
      expect.anything(),
    )
  })

  it('shows an empty state when no installers are found', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<BrowsePage />)
    await userEvent.type(screen.getByLabelText(/latitude/i), '0')
    await userEvent.type(screen.getByLabelText(/longitude/i), '0')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))

    await waitFor(() => expect(screen.getByText(/no verified installers/i)).toBeInTheDocument())
  })
})
