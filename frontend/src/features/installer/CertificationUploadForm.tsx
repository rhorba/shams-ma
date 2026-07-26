import { useRef, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { ApiError, apiFetch } from '../../lib/api'

interface CertificationSummary {
  id: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  uploadedAt: string
}

export default function CertificationUploadForm() {
  const { accessToken } = useAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [uploaded, setUploaded] = useState<CertificationSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleFileChange = async () => {
    const file = fileInputRef.current?.files?.[0]
    if (!file) return

    setError(null)
    setSubmitting(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const summary = await apiFetch<CertificationSummary>('/api/v1/installer/certifications', {
        method: 'POST',
        accessToken,
        body: formData,
        isFormData: true,
      })
      setUploaded(summary)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Upload failed')
    } finally {
      setSubmitting(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Certification documents
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Upload a PDF, JPEG or PNG (max 10MB) for admin verification.
      </Typography>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {uploaded && (
        <Box sx={{ mb: 2 }}>
          <Chip label={uploaded.status} color={uploaded.status === 'PENDING' ? 'warning' : 'success'} />
        </Box>
      )}
      <input
        ref={fileInputRef}
        type="file"
        accept=".pdf,.jpg,.jpeg,.png"
        style={{ display: 'none' }}
        onChange={handleFileChange}
      />
      <Button variant="contained" disabled={submitting} onClick={() => fileInputRef.current?.click()}>
        {submitting ? 'Uploading…' : 'Upload certification'}
      </Button>
    </Paper>
  )
}
