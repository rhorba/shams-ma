import { useState } from 'react'
import { keepPreviousData, useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import FormControlLabel from '@mui/material/FormControlLabel'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch } from '../../lib/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type BookingStatus = 'PENDING_PAYMENT' | 'BOOKED' | 'CANCELLED'
type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED'

interface AdminBookingOverviewRow {
  bookingId: string
  bookingStatus: BookingStatus
  depositAmount: number
  bookingCreatedAt: string
  homeownerName: string
  installerBusinessName: string
  paymentStatus: PaymentStatus | null
  paymentAmount: number | null
  cmiTransactionId: string | null
  openFlagId: string | null
  openFlagReason: string | null
  openFlagExpectedAmount: number | null
  openFlagActualAmount: number | null
}

interface Page<T> {
  content: T[]
  totalElements: number
}

export default function AdminBookingsOverviewPage() {
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()

  const [bookingStatus, setBookingStatus] = useState('')
  const [paymentStatus, setPaymentStatus] = useState('')
  const [needsReviewOnly, setNeedsReviewOnly] = useState(false)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(20)

  const queryParams = new URLSearchParams({
    needsReviewOnly: String(needsReviewOnly),
    page: String(page),
    size: String(rowsPerPage),
  })
  if (bookingStatus) queryParams.set('bookingStatus', bookingStatus)
  if (paymentStatus) queryParams.set('paymentStatus', paymentStatus)
  if (search) queryParams.set('search', search)

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-bookings', bookingStatus, paymentStatus, needsReviewOnly, search, page, rowsPerPage],
    queryFn: () =>
      apiFetch<Page<AdminBookingOverviewRow>>(`/api/v1/admin/bookings?${queryParams}`, { accessToken }),
    placeholderData: keepPreviousData,
  })

  const reviewFlag = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'resolve' | 'dismiss' }) =>
      apiFetch<void>(`/api/v1/admin/payment-flags/${id}/${action}`, { method: 'POST', accessToken }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-bookings'] }),
  })

  const handleExport = async () => {
    const response = await fetch(`${API_BASE_URL}/api/v1/admin/bookings/export?${queryParams}`, {
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    })
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'bookings.csv'
    link.click()
    URL.revokeObjectURL(url)
  }

  if (isLoading) return <CircularProgress />
  if (error) return <Alert severity="error">Could not load bookings.</Alert>

  return (
    <Stack spacing={2} sx={{ maxWidth: 1200, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Booking &amp; payment overview</Typography>

      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          select
          label="Booking status"
          size="small"
          value={bookingStatus}
          onChange={(e) => {
            setBookingStatus(e.target.value)
            setPage(0)
          }}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="PENDING_PAYMENT">Pending payment</MenuItem>
          <MenuItem value="BOOKED">Booked</MenuItem>
          <MenuItem value="CANCELLED">Cancelled</MenuItem>
        </TextField>
        <TextField
          select
          label="Payment status"
          size="small"
          value={paymentStatus}
          onChange={(e) => {
            setPaymentStatus(e.target.value)
            setPage(0)
          }}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="PENDING">Pending</MenuItem>
          <MenuItem value="SUCCEEDED">Succeeded</MenuItem>
          <MenuItem value="FAILED">Failed</MenuItem>
          <MenuItem value="REFUNDED">Refunded</MenuItem>
        </TextField>
        <FormControlLabel
          control={
            <Checkbox
              checked={needsReviewOnly}
              onChange={(e) => {
                setNeedsReviewOnly(e.target.checked)
                setPage(0)
              }}
            />
          }
          label="Needs review only"
        />
        <TextField
          label="Search"
          size="small"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPage(0)
          }}
        />
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="outlined" onClick={handleExport}>
          Export CSV
        </Button>
      </Stack>

      {data?.content.length === 0 && <Alert severity="info">No bookings match these filters.</Alert>}

      {data && data.content.length > 0 && (
        <>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Homeowner</TableCell>
                <TableCell>Installer</TableCell>
                <TableCell>Booking status</TableCell>
                <TableCell>Deposit</TableCell>
                <TableCell>Payment status</TableCell>
                <TableCell>Review</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data.content.map((row) => (
                <TableRow key={row.bookingId}>
                  <TableCell>{row.homeownerName}</TableCell>
                  <TableCell>{row.installerBusinessName}</TableCell>
                  <TableCell>
                    <Chip label={row.bookingStatus} size="small" />
                  </TableCell>
                  <TableCell>{row.depositAmount}</TableCell>
                  <TableCell>
                    {row.paymentStatus ? <Chip label={row.paymentStatus} size="small" /> : '—'}
                  </TableCell>
                  <TableCell>
                    {row.openFlagId ? (
                      <Stack spacing={0.5}>
                        <Typography variant="body2" color="error">
                          {row.openFlagReason}: expected {row.openFlagExpectedAmount}, got{' '}
                          {row.openFlagActualAmount}
                        </Typography>
                        <Stack direction="row" spacing={1}>
                          <Button
                            size="small"
                            variant="contained"
                            disabled={reviewFlag.isPending}
                            onClick={() => reviewFlag.mutate({ id: row.openFlagId as string, action: 'resolve' })}
                          >
                            Resolve
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            disabled={reviewFlag.isPending}
                            onClick={() => reviewFlag.mutate({ id: row.openFlagId as string, action: 'dismiss' })}
                          >
                            Dismiss
                          </Button>
                        </Stack>
                      </Stack>
                    ) : (
                      '—'
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={data.totalElements}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(Number(e.target.value))
              setPage(0)
            }}
            rowsPerPageOptions={[10, 20, 50]}
          />
        </>
      )}
    </Stack>
  )
}
