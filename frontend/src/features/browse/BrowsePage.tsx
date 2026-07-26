import { useState } from 'react'
import type { FormEvent } from 'react'
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

export default function BrowsePage() {
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null)
  const [latInput, setLatInput] = useState('')
  const [lngInput, setLngInput] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['browse', coords],
    queryFn: () =>
      apiFetch<InstallerBrowseResult[]>(
        `/api/v1/installers/browse?lat=${coords!.lat}&lng=${coords!.lng}`,
      ),
    enabled: coords !== null,
  })

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    setCoords({ lat: Number(latInput), lng: Number(lngInput) })
  }

  const useMyLocation = () => {
    navigator.geolocation.getCurrentPosition((position) => {
      setLatInput(String(position.coords.latitude))
      setLngInput(String(position.coords.longitude))
      setCoords({ lat: position.coords.latitude, lng: position.coords.longitude })
    })
  }

  return (
    <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Find a solar installer</Typography>
      <Box component="form" onSubmit={handleSubmit}>
        <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <TextField
            label="Latitude"
            value={latInput}
            onChange={(e) => setLatInput(e.target.value)}
            size="small"
            required
          />
          <TextField
            label="Longitude"
            value={lngInput}
            onChange={(e) => setLngInput(e.target.value)}
            size="small"
            required
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
