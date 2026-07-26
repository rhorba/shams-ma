import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CoverageZoneForm from './CoverageZoneForm'
import { renderWithProviders } from '../../test-utils'

// jsdom has no canvas/ResizeObserver support Leaflet needs — the map's own rendering isn't the
// unit under test here, the form/submit logic is, so stub it out to a plain marker element.
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children?: React.ReactNode }) => <div data-testid="map">{children}</div>,
  TileLayer: () => null,
  Marker: () => <div data-testid="marker" />,
  Circle: () => null,
}))

describe('CoverageZoneForm', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('submits the address/radius and shows the resolved coordinates', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ lat: 34.02, lng: -6.84, radiusKm: 25 }), { status: 200 }),
    )

    renderWithProviders(<CoverageZoneForm />)
    await userEvent.type(screen.getByLabelText(/base address/i), 'Rabat, Morocco')
    await userEvent.click(screen.getByRole('button', { name: /save coverage zone/i }))

    await waitFor(() => expect(screen.getByText(/center resolved to 34.0200, -6.8400/i)).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/installer/coverage-zone'),
      expect.objectContaining({ method: 'PUT' }),
    )
  })

  it('shows an error when the address cannot be geocoded', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Could not resolve address' }), { status: 400 }),
    )

    renderWithProviders(<CoverageZoneForm />)
    await userEvent.type(screen.getByLabelText(/base address/i), 'Nowhere')
    await userEvent.click(screen.getByRole('button', { name: /save coverage zone/i }))

    await waitFor(() => expect(screen.getByText('Could not resolve address')).toBeInTheDocument())
  })
})
