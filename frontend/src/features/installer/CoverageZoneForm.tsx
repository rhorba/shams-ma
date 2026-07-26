import { useState } from 'react'
import type { FormEvent } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Slider from '@mui/material/Slider'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { MapContainer, TileLayer, Marker, Circle } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch, ApiError } from '../../lib/api'

// Vite doesn't resolve Leaflet's default marker asset paths correctly; point at the CDN instead.
const markerIcon = new L.Icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
})

interface CoverageZoneResponse {
  lat: number
  lng: number
  radiusKm: number
}

const MOROCCO_CENTER: [number, number] = [31.7917, -7.0926]

export default function CoverageZoneForm() {
  const { accessToken } = useAuth()
  const [addressText, setAddressText] = useState('')
  const [radiusKm, setRadiusKm] = useState(25)
  const [result, setResult] = useState<CoverageZoneResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const response = await apiFetch<CoverageZoneResponse>('/api/v1/installer/coverage-zone', {
        method: 'PUT',
        accessToken,
        body: JSON.stringify({ addressText, radiusKm }),
      })
      setResult(response)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not set coverage zone')
    } finally {
      setSubmitting(false)
    }
  }

  const center: [number, number] = result ? [result.lat, result.lng] : MOROCCO_CENTER

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Coverage zone
      </Typography>
      <Box component="form" onSubmit={handleSubmit} sx={{ mb: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {result && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Coverage zone set — center resolved to {result.lat.toFixed(4)}, {result.lng.toFixed(4)}
          </Alert>
        )}
        <TextField
          label="Base address"
          fullWidth
          margin="normal"
          value={addressText}
          onChange={(e) => setAddressText(e.target.value)}
          placeholder="e.g. Rabat, Morocco"
          required
        />
        <Typography gutterBottom sx={{ mt: 2 }}>
          Radius: {radiusKm} km
        </Typography>
        <Slider
          value={radiusKm}
          onChange={(_, value) => setRadiusKm(value as number)}
          min={1}
          max={200}
          valueLabelDisplay="auto"
        />
        <Button type="submit" variant="contained" disabled={submitting} sx={{ mt: 2 }}>
          {submitting ? 'Saving…' : 'Save coverage zone'}
        </Button>
      </Box>

      <Box sx={{ height: 320, borderRadius: 1, overflow: 'hidden' }}>
        <MapContainer center={center} zoom={result ? 9 : 5} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {result && (
            <>
              <Marker position={center} icon={markerIcon} />
              <Circle center={center} radius={result.radiusKm * 1000} />
            </>
          )}
        </MapContainer>
      </Box>
    </Paper>
  )
}
