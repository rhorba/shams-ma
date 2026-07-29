import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Grid from '@mui/material/Grid'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { LineChart } from '@mui/x-charts/LineChart'
import { ChartsReferenceLine } from '@mui/x-charts/ChartsReferenceLine'
import { apiFetch } from '../../lib/api'

type RoiOrientation = 'SOUTH' | 'EAST_WEST' | 'NORTH' | 'FLAT_UNKNOWN'

const ORIENTATION_OPTIONS: { value: RoiOrientation; label: string }[] = [
  { value: 'SOUTH', label: 'South-facing (optimal)' },
  { value: 'EAST_WEST', label: 'East or west-facing' },
  { value: 'NORTH', label: 'North-facing' },
  { value: 'FLAT_UNKNOWN', label: "Flat roof / not sure" },
]

interface RoiEstimateResponse {
  lat: number
  lng: number
  resolvedCity: string
  estimatedSystemKwp: number
  annualProductionKwh: number
  installationCostMad: number
  annualSavingsMad: number
  paybackYears: number | null
}

interface RoiQueryParams {
  address: string
  monthlyBillMad: number
  roofSizeM2: string
  orientation: RoiOrientation | ''
}

const PROJECTION_YEARS = 20

function buildQueryString(params: RoiQueryParams): string {
  const search = new URLSearchParams({
    address: params.address,
    monthlyBillMad: String(params.monthlyBillMad),
  })
  if (params.roofSizeM2) {
    search.set('roofSizeM2', params.roofSizeM2)
  }
  if (params.orientation) {
    search.set('orientation', params.orientation)
  }
  return search.toString()
}

export default function RoiCalculatorPage() {
  const navigate = useNavigate()
  const [address, setAddress] = useState('')
  const [monthlyBillMad, setMonthlyBillMad] = useState('')
  const [roofSizeM2, setRoofSizeM2] = useState('')
  const [orientation, setOrientation] = useState<RoiOrientation | ''>('')
  const [params, setParams] = useState<RoiQueryParams | null>(null)

  const { data, isLoading, error } = useQuery({
    queryKey: ['roi-estimate', params],
    queryFn: () =>
      apiFetch<RoiEstimateResponse>(`/api/v1/roi/estimate?${buildQueryString(params!)}`),
    enabled: params !== null,
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setParams({ address, monthlyBillMad: Number(monthlyBillMad), roofSizeM2, orientation })
  }

  const chartSeries =
    data && data.paybackYears !== null
      ? Array.from({ length: PROJECTION_YEARS + 1 }, (_, year) => ({
          year,
          netCashFlowMad: year * data.annualSavingsMad - data.installationCostMad,
        }))
      : null

  return (
    <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Estimate your solar savings</Typography>
      <Paper sx={{ p: 3 }} component="form" onSubmit={handleSubmit}>
        <Stack spacing={2}>
          <TextField
            label="Address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="e.g. Rabat, Morocco"
            required
          />
          <TextField
            label="Monthly energy bill (MAD)"
            type="number"
            value={monthlyBillMad}
            onChange={(e) => setMonthlyBillMad(e.target.value)}
            required
            slotProps={{ htmlInput: { min: 1, step: '0.01' } }}
          />
          <TextField
            label="Roof size (m²) — optional"
            type="number"
            value={roofSizeM2}
            onChange={(e) => setRoofSizeM2(e.target.value)}
            slotProps={{ htmlInput: { min: 1, step: '0.01' } }}
          />
          <TextField
            select
            label="Roof orientation — optional"
            value={orientation}
            onChange={(e) => setOrientation(e.target.value as RoiOrientation | '')}
          >
            <MenuItem value="">Not sure</MenuItem>
            {ORIENTATION_OPTIONS.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </TextField>
          <Button type="submit" variant="contained">
            Calculate ROI
          </Button>
        </Stack>
      </Paper>

      {isLoading && <CircularProgress />}
      {error && <Alert severity="error">Could not calculate an estimate for that address.</Alert>}

      {data && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Based on typical irradiance for {data.resolvedCity}
          </Typography>
          <Grid container spacing={3} sx={{ mb: 2 }}>
            <Grid size={{ xs: 6, sm: 3 }}>
              <Typography variant="caption" color="text.secondary">
                Estimated payback
              </Typography>
              <Typography variant="h5">
                {data.paybackYears !== null ? `${data.paybackYears} yrs` : 'N/A'}
              </Typography>
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <Typography variant="caption" color="text.secondary">
                Annual savings
              </Typography>
              <Typography variant="h5">{data.annualSavingsMad.toLocaleString()} MAD</Typography>
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <Typography variant="caption" color="text.secondary">
                System size
              </Typography>
              <Typography variant="h5">{data.estimatedSystemKwp} kWp</Typography>
            </Grid>
            <Grid size={{ xs: 6, sm: 3 }}>
              <Typography variant="caption" color="text.secondary">
                Installation cost
              </Typography>
              <Typography variant="h5">{data.installationCostMad.toLocaleString()} MAD</Typography>
            </Grid>
          </Grid>

          {chartSeries ? (
            <Box aria-label="20-year net cash flow projection">
              <LineChart
                height={280}
                dataset={chartSeries}
                xAxis={[{ dataKey: 'year', label: 'Years since installation' }]}
                yAxis={[{ label: 'Net cash flow (MAD)' }]}
                series={[
                  {
                    dataKey: 'netCashFlowMad',
                    label: 'Cumulative net savings',
                    color: '#1E7A4C',
                    area: true,
                    showMark: false,
                  },
                ]}
              >
                <ChartsReferenceLine
                  y={0}
                  lineStyle={{ strokeDasharray: '4 4' }}
                  label="Break-even"
                />
              </LineChart>
            </Box>
          ) : (
            <Alert severity="warning">
              This system doesn't pay for itself within {PROJECTION_YEARS} years at this bill
              level.
            </Alert>
          )}

          <Button
            variant="text"
            sx={{ mt: 2 }}
            onClick={() => navigate(`/browse?lat=${data.lat}&lng=${data.lng}`)}
          >
            See verified installers →
          </Button>
        </Paper>
      )}
    </Stack>
  )
}
