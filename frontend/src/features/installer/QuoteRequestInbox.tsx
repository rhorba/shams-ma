import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch } from '../../lib/api'

interface QuoteRequestSummary {
  id: string
  homeownerId: string
  homeownerFullName: string
  status: 'REQUESTED' | 'QUOTED' | 'DECLINED' | 'BOOKED'
  message: string | null
  quoteAmount: number | null
  createdAt: string
}

const STATUS_COLOR: Record<QuoteRequestSummary['status'], 'default' | 'success' | 'error' | 'info'> = {
  REQUESTED: 'default',
  QUOTED: 'info',
  DECLINED: 'error',
  BOOKED: 'success',
}

export default function QuoteRequestInbox() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()
  const [drafts, setDrafts] = useState<Record<string, string>>({})

  const { data, isLoading, error } = useQuery({
    queryKey: ['installer-quote-requests'],
    queryFn: () =>
      apiFetch<QuoteRequestSummary[]>('/api/v1/installer/quote-requests', { accessToken }),
  })

  const respond = useMutation({
    mutationFn: ({
      id,
      action,
      quoteAmount,
    }: {
      id: string
      action: 'QUOTE' | 'DECLINE'
      quoteAmount?: number
    }) =>
      apiFetch(`/api/v1/installer/quote-requests/${id}/respond`, {
        method: 'POST',
        accessToken,
        body: JSON.stringify({ action, quoteAmount }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['installer-quote-requests'] }),
  })

  if (isLoading) return <CircularProgress />
  if (error) return <Alert severity="error">Could not load your leads.</Alert>

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" gutterBottom>
        Lead inbox
      </Typography>
      {data?.length === 0 && <Alert severity="info">No quote requests yet.</Alert>}
      {data && data.length > 0 && (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Homeowner</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Message</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map((request) => (
              <TableRow key={request.id}>
                <TableCell>{request.homeownerFullName}</TableCell>
                <TableCell>
                  <Chip label={request.status} color={STATUS_COLOR[request.status]} size="small" />
                </TableCell>
                <TableCell>{request.message ?? '—'}</TableCell>
                <TableCell>
                  {request.status === 'REQUESTED' ? (
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                      <TextField
                        size="small"
                        type="number"
                        label="Amount (MAD)"
                        value={drafts[request.id] ?? ''}
                        onChange={(e) =>
                          setDrafts((prev) => ({ ...prev, [request.id]: e.target.value }))
                        }
                        sx={{ width: 140 }}
                      />
                      <Button
                        size="small"
                        variant="contained"
                        disabled={respond.isPending || !drafts[request.id]}
                        onClick={() =>
                          respond.mutate({
                            id: request.id,
                            action: 'QUOTE',
                            quoteAmount: Number(drafts[request.id]),
                          })
                        }
                      >
                        Send quote
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        disabled={respond.isPending}
                        onClick={() => respond.mutate({ id: request.id, action: 'DECLINE' })}
                      >
                        Decline
                      </Button>
                    </Stack>
                  ) : (
                    request.quoteAmount !== null && `${request.quoteAmount.toLocaleString()} MAD`
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Paper>
  )
}
