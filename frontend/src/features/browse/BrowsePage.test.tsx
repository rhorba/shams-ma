import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect, useState } from 'react'
import BrowsePage from './BrowsePage'
import { useAuth } from '../../auth/AuthContext'
import { renderWithProviders } from '../../test-utils'

// Same test-only login-seeding pattern as auth/RequireRole.test.tsx.
function LoggedInThen({
  email,
  password,
  children,
}: {
  email: string
  password: string
  children: React.ReactNode
}) {
  const { login, role } = useAuth()
  const [ready, setReady] = useState(false)
  useEffect(() => {
    login(email, password).then(() => setReady(true))
    // Intentionally run once on mount — this is test-only login-seeding, not app code.
  }, [])
  if (!ready || !role) return null
  return <>{children}</>
}

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

  it('lets a logged-in homeowner request a quote from a listed installer', async () => {
    vi.mocked(fetch)
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            accessToken: 't',
            tokenType: 'Bearer',
            expiresIn: 900,
            role: 'HOMEOWNER',
            mfaEnrolled: false,
          }),
          { status: 200 },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify([
            { userId: 'inst-1', businessName: 'Solaire Atlas', phone: null, distanceKm: 2.5 },
          ]),
          { status: 200 },
        ),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify([{ id: 'qr-1' }]), { status: 201 }))

    renderWithProviders(
      <LoggedInThen email="homeowner@example.com" password="pw">
        <BrowsePage />
      </LoggedInThen>,
    )

    await userEvent.type(await screen.findByLabelText(/address/i), 'Rabat, Morocco')
    await userEvent.click(screen.getByRole('button', { name: /^search$/i }))
    await waitFor(() => expect(screen.getByText('Solaire Atlas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /request quote/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/homeowner/quote-requests'),
        expect.objectContaining({ method: 'POST' }),
      ),
    )
    expect(await screen.findByText(/quote requested/i)).toBeInTheDocument()
  })
})
