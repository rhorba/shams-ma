import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useAuth } from '../../auth/AuthContext'
import { apiFetch } from '../../lib/api'

interface BookingResponse {
  id: string
  quoteRequestId: string
  status: 'PENDING_PAYMENT' | 'BOOKED' | 'CANCELLED'
  depositAmount: number
  createdAt: string
}

interface CheckoutResponse {
  paymentId: string
  checkoutUrl: string
  cmiTransactionId: string
  amount: number
  currency: string
}

export default function PaymentCheckoutPage() {
  const { bookingId } = useParams<{ bookingId: string }>()
  const { accessToken } = useAuth()
  const queryClient = useQueryClient()

  const bookingQuery = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () =>
      apiFetch<BookingResponse>(`/api/v1/homeowner/bookings/${bookingId}`, { accessToken }),
  })

  const checkout = useMutation({
    mutationFn: () =>
      apiFetch<CheckoutResponse>(`/api/v1/homeowner/bookings/${bookingId}/checkout`, {
        method: 'POST',
        accessToken,
      }),
  })

  const simulate = useMutation({
    mutationFn: (outcome: 'succeed' | 'fail') =>
      apiFetch(`/api/v1/mock-cmi/${checkout.data!.cmiTransactionId}/${outcome}`, {
        method: 'POST',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['booking', bookingId] })
      checkout.reset()
    },
  })

  if (bookingQuery.isLoading) return <CircularProgress />
  if (bookingQuery.error) return <Alert severity="error">Could not load this booking.</Alert>

  const booking = bookingQuery.data!

  return (
    <Stack spacing={3} sx={{ maxWidth: 560, mx: 'auto', p: 3 }}>
      <Typography variant="h4">Booking payment</Typography>
      <Paper sx={{ p: 3 }}>
        {booking.status === 'BOOKED' && (
          <Alert severity="success">Payment received — your booking is confirmed!</Alert>
        )}

        {booking.status === 'PENDING_PAYMENT' && !checkout.data && (
          <Stack spacing={2}>
            <Typography>Deposit due: {booking.depositAmount.toLocaleString()} MAD</Typography>
            <Button
              variant="contained"
              disabled={checkout.isPending}
              onClick={() => checkout.mutate()}
            >
              {checkout.isPending ? 'Starting checkout…' : 'Proceed to payment'}
            </Button>
            {checkout.isError && <Alert severity="error">Could not start checkout.</Alert>}
          </Stack>
        )}

        {booking.status === 'PENDING_PAYMENT' && checkout.data && (
          <Stack spacing={2}>
            <Typography>
              Deposit due: {checkout.data.amount.toLocaleString()} {checkout.data.currency}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Mock CMI checkout — no real payment gateway is connected yet.
            </Typography>
            <Stack direction="row" spacing={2}>
              <Button
                variant="contained"
                color="success"
                disabled={simulate.isPending}
                onClick={() => simulate.mutate('succeed')}
              >
                Simulate successful payment
              </Button>
              <Button
                variant="outlined"
                color="error"
                disabled={simulate.isPending}
                onClick={() => simulate.mutate('fail')}
              >
                Simulate failed payment
              </Button>
            </Stack>
            {simulate.isError && <Alert severity="error">Simulation failed.</Alert>}
          </Stack>
        )}
      </Paper>
    </Stack>
  )
}
