import { useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch } from '../../lib/api'

interface QuoteRequestSummary {
  id: string
  installerId: string
  installerBusinessName: string
  status: 'REQUESTED' | 'QUOTED' | 'DECLINED' | 'BOOKED'
  quoteAmount: number | null
  quoteNotes: string | null
  createdAt: string
}

const STATUS_COLOR: Record<QuoteRequestSummary['status'], 'default' | 'success' | 'error' | 'info'> = {
  REQUESTED: 'default',
  QUOTED: 'info',
  DECLINED: 'error',
  BOOKED: 'success',
}

export default function HomeownerRequestsPage() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data, isLoading, error } = useQuery({
    queryKey: ['homeowner-quote-requests'],
    queryFn: () =>
      apiFetch<QuoteRequestSummary[]>('/api/v1/homeowner/quote-requests', { accessToken }),
  })

  const book = useMutation({
    mutationFn: (id: string) =>
      apiFetch<{ id: string }>(`/api/v1/homeowner/quote-requests/${id}/book`, {
        method: 'POST',
        accessToken,
      }),
    onSuccess: (booking) => {
      queryClient.invalidateQueries({ queryKey: ['homeowner-quote-requests'] })
      navigate(`/homeowner/bookings/${booking.id}/checkout`)
    },
  })

  if (isLoading) return <CircularProgress />
  if (error) return <Alert severity="error">Could not load your quote requests.</Alert>

  return (
    <Stack spacing={2} sx={{ maxWidth: 960, mx: 'auto', p: 3 }}>
      <Typography variant="h4">My quote requests</Typography>
      {data?.length === 0 && <Alert severity="info">You haven't requested any quotes yet.</Alert>}
      {data && data.length > 0 && (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Installer</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Quote</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((request) => (
              <TableRow key={request.id}>
                <TableCell>{request.installerBusinessName}</TableCell>
                <TableCell>
                  <Chip label={request.status} color={STATUS_COLOR[request.status]} size="small" />
                </TableCell>
                <TableCell>
                  {request.quoteAmount !== null
                    ? `${request.quoteAmount.toLocaleString()} MAD`
                    : '—'}
                  {request.quoteNotes ? ` · ${request.quoteNotes}` : ''}
                </TableCell>
                <TableCell>
                  {request.status === 'QUOTED' && (
                    <Button
                      size="small"
                      variant="contained"
                      disabled={book.isPending}
                      onClick={() => book.mutate(request.id)}
                    >
                      Book
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  )
}
