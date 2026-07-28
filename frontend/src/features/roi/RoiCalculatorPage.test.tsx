import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useSearchParams } from 'react-router-dom'
import RoiCalculatorPage from './RoiCalculatorPage'
import { renderWithProviders } from '../../test-utils'

function BrowseStub() {
  const [params] = useSearchParams()
  return (
    <div>
      browse page: lat={params.get('lat')} lng={params.get('lng')}
    </div>
  )
}

function RoiRoutes() {
  return (
    <Routes>
      <Route path="/roi" element={<RoiCalculatorPage />} />
      <Route path="/browse" element={<BrowseStub />} />
    </Routes>
  )
}

const VIABLE_ESTIMATE = {
  lat: 34.0209,
  lng: -6.8416,
  resolvedCity: 'Rabat',
  estimatedSystemKwp: 3.34,
  annualProductionKwh: 5069.53,
  installationCostMad: 40064.8,
  annualSavingsMad: 6000,
  paybackYears: 6.68,
}

describe('RoiCalculatorPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  async function fillAndSubmit(address = 'Rabat, Morocco', monthlyBill = '500') {
    await userEvent.type(screen.getByLabelText(/address/i), address)
    await userEvent.type(screen.getByLabelText(/monthly energy bill/i), monthlyBill)
    await userEvent.click(screen.getByRole('button', { name: /calculate roi/i }))
  }

  // Mounting the chart (SVG layout + MUI transitions) is heavier than a plain form and can
  // cross vitest's default 5000ms per-test budget under this sandbox's slower parallel runs
  // (see LoginPage.test.tsx's navigation test for the same class of flake) — same
  // mitigation as RequireRole.test.tsx's slower async flows: an explicit longer timeout.
  const SLOW_TEST_TIMEOUT = 10000

  it(
    'calculates and displays the estimate',
    async () => {
      vi.mocked(fetch).mockResolvedValueOnce(
        new Response(JSON.stringify(VIABLE_ESTIMATE), { status: 200 }),
      )

      renderWithProviders(<RoiRoutes />, ['/roi'])
      await fillAndSubmit()

      await waitFor(() => expect(screen.getByText(/6.68 yrs/)).toBeInTheDocument(), {
        timeout: SLOW_TEST_TIMEOUT,
      })
      expect(screen.getByText(/based on typical irradiance for rabat/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/20-year net cash flow projection/i)).toBeInTheDocument()
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining(
          '/api/v1/roi/estimate?address=Rabat%2C+Morocco&monthlyBillMad=500',
        ),
        expect.anything(),
      )
    },
    SLOW_TEST_TIMEOUT,
  )

  it(
    'shows a warning instead of a chart when the system never pays back',
    async () => {
      vi.mocked(fetch).mockResolvedValueOnce(
        new Response(JSON.stringify({ ...VIABLE_ESTIMATE, paybackYears: null }), { status: 200 }),
      )

      renderWithProviders(<RoiRoutes />, ['/roi'])
      await fillAndSubmit()

      await waitFor(() => expect(screen.getByText(/doesn't pay for itself/i)).toBeInTheDocument(), {
        timeout: SLOW_TEST_TIMEOUT,
      })
      expect(screen.queryByLabelText(/20-year net cash flow projection/i)).not.toBeInTheDocument()
    },
    SLOW_TEST_TIMEOUT,
  )

  it('shows an error message when the address cannot be resolved', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ message: 'Could not resolve address' }), { status: 400 }),
    )

    renderWithProviders(<RoiRoutes />, ['/roi'])
    await fillAndSubmit('nonsense address')

    await waitFor(() =>
      expect(screen.getByText(/could not calculate an estimate/i)).toBeInTheDocument(),
    )
  })

  it(
    'deep-links into browse with the geocoded coordinates',
    async () => {
      vi.mocked(fetch).mockResolvedValueOnce(
        new Response(JSON.stringify(VIABLE_ESTIMATE), { status: 200 }),
      )

      renderWithProviders(<RoiRoutes />, ['/roi'])
      await fillAndSubmit()
      await waitFor(() => expect(screen.getByText(/6.68 yrs/)).toBeInTheDocument(), {
        timeout: SLOW_TEST_TIMEOUT,
      })

      await userEvent.click(screen.getByRole('button', { name: /see verified installers/i }))

      await waitFor(() =>
        expect(screen.getByText('browse page: lat=34.0209 lng=-6.8416')).toBeInTheDocument(),
      )
    },
    SLOW_TEST_TIMEOUT,
  )

  it('sends optional roof size and orientation when provided', async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify(VIABLE_ESTIMATE), { status: 200 }),
    )

    renderWithProviders(<RoiRoutes />, ['/roi'])
    await userEvent.type(screen.getByLabelText(/address/i), 'Rabat, Morocco')
    await userEvent.type(screen.getByLabelText(/monthly energy bill/i), '500')
    await userEvent.type(screen.getByLabelText(/roof size/i), '30')
    await userEvent.click(screen.getByLabelText(/roof orientation/i))
    await userEvent.click(await screen.findByRole('option', { name: /south-facing/i }))
    await userEvent.click(screen.getByRole('button', { name: /calculate roi/i }))

    await waitFor(() =>
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('roofSizeM2=30'),
        expect.anything(),
      ),
    )
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('orientation=SOUTH'),
      expect.anything(),
    )
  })
})
