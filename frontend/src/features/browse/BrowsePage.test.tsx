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

  it('searches by address and renders results', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify([
          { userId: 'inst-1', businessName: 'Solaire Atlas', phone: '+212600000000', distanceKm: 4.2 },
        ]),
        { status: 200 },
      ),
    )

    renderWithProviders(<BrowsePage />)
    await userEvent.type(screen.getByLabelText(/address/i), 'Rabat, Morocco')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))

    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/installers/browse?address=Rabat%2C%20Morocco'),
      expect.anything(),
    )
  })

  it('shows an empty state when no installers are found', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))

    renderWithProviders(<BrowsePage />)
    await userEvent.type(screen.getByLabelText(/address/i), 'Nowhere')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))

    await waitFor(() => expect(screen.getByText(/no verified installers/i)).toBeInTheDocument())
  })

  it('auto-searches using lat/lng passed in the URL (ROI calculator deep link)', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(
        JSON.stringify([
          { userId: 'inst-2', businessName: 'Solaire Nord', phone: null, distanceKm: 1.1 },
        ]),
        { status: 200 },
      ),
    )

    renderWithProviders(<BrowsePage />, ['/browse?lat=34.0209&lng=-6.8416'])

    await waitFor(() => expect(screen.getByText('Solaire Nord')).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/installers/browse?lat=34.0209&lng=-6.8416'),
      expect.anything(),
    )
  })

  it('searches using the browser geolocation position', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200 }))
    const getCurrentPosition = vi.fn((success: PositionCallback) =>
      success({ coords: { latitude: 30.4278, longitude: -9.5981 } } as GeolocationPosition),
    )
    vi.stubGlobal('navigator', { ...navigator, geolocation: { getCurrentPosition } })

    renderWithProviders(<BrowsePage />)
    await userEvent.click(screen.getByRole('button', { name: /use my location/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/installers/browse?lat=30.4278&lng=-9.5981'),
        expect.anything(),
      ),
    )
  })
})
