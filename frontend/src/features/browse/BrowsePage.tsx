import { useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { apiFetch } from '../../lib/api'

interface InstallerBrowseResult {
  userId: string
  businessName: string
  phone: string | null
  distanceKm: number
}

type BrowseQuery = { address: string } | { lat: number; lng: number }

function buildQueryString(query: BrowseQuery): string {
  if ('address' in query) {
    return `address=${encodeURIComponent(query.address)}`
  }
  return `lat=${query.lat}&lng=${query.lng}`
}

function initialQueryFromUrl(searchParams: URLSearchParams): BrowseQuery | null {
  const lat = searchParams.get('lat')
  const lng = searchParams.get('lng')
  return lat !== null && lng !== null ? { lat: Number(lat), lng: Number(lng) } : null
}

export default function BrowsePage() {
  const [searchParams] = useSearchParams()
  const [address, setAddress] = useState('')
  const [query, setQuery] = useState<BrowseQuery | null>(() => initialQueryFromUrl(searchParams))

  const { data, isLoading, error } = useQuery({
    queryKey: ['browse', query],
    queryFn: () =>
      apiFetch<InstallerBrowseResult[]>(`/api/v1/installers/browse?${buildQueryString(query!)}`),
    enabled: query !== null,
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setQuery({ address })
  }

  const useMyLocation = () => {
    navigator.geolocation.getCurrentPosition((position) => {
      setQuery({ lat: position.coords.latitude, lng: position.coords.longitude })
    })
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Find a solar installer</Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            label="Address"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            placeholder="e.g. Rabat, Morocco"
            size="small"
            required
            sx={{ minWidth: 260 }}
          />
          <Button type="submit" variant="contained">
            Search
          </Button>
          <Button variant="text" onClick={useMyLocation}>
            Use my location
          </Button>
        </Stack>
      </Box>

      {isLoading && <CircularProgress />}
      {error && <Alert severity="error">Could not load installers.</Alert>}
      {data?.length === 0 && <Alert severity="info">No verified installers found nearby.</Alert>}
      {data?.map((installer) => (
        <Card key={installer.userId}>
          <CardContent>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h6">{installer.businessName}</Typography>
              <Chip label="Verified" color="success" size="small" />
            </Stack>
            <Typography variant="body2" color="text.secondary">
              {installer.distanceKm.toFixed(1)} km away
              {installer.phone ? ` · ${installer.phone}` : ''}
            </Typography>
          </CardContent>
        </Card>
      ))}
    </Stack>
  )
}
