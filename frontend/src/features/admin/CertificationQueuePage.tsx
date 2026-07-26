import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Link from '@mui/material/Link'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch } from '../../lib/api'

interface CertificationSummary {
  id: string
  installerId: string
  businessName: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  viewUrl: string
  uploadedAt: string
}

export default function CertificationQueuePage() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-certifications', 'PENDING'],
    queryFn: () =>
      apiFetch<CertificationSummary[]>('/api/v1/admin/certifications?status=PENDING', { accessToken }),
  })

  const review = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'approve' | 'reject' }) =>
      apiFetch<void>(`/api/v1/admin/certifications/${id}/${action}`, { method: 'POST', accessToken }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-certifications'] }),
  })

  if (isLoading) return <CircularProgress />
  if (error) return <Alert severity="error">Could not load the review queue.</Alert>

  return (
    <Stack spacing={2} sx={{ maxWidth: 960, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Certification review queue</Typography>
      {data?.length === 0 && <Alert severity="info">No pending certifications.</Alert>}
      {data && data.length > 0 && (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Installer</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Document</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((cert) => (
              <TableRow key={cert.id}>
                <TableCell>{cert.businessName}</TableCell>
                <TableCell>
                  <Chip label={cert.status} size="small" />
                </TableCell>
                <TableCell>
                  <Link href={cert.viewUrl} target="_blank" rel="noopener noreferrer">
                    View
                  </Link>
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1}>
                    <Button
                      size="small"
                      variant="contained"
                      color="success"
                      disabled={review.isPending}
                      onClick={() => review.mutate({ id: cert.id, action: 'approve' })}
                    >
                      Approve
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color="error"
                      disabled={review.isPending}
                      onClick={() => review.mutate({ id: cert.id, action: 'reject' })}
                    >
                      Reject
                    </Button>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  )
}
