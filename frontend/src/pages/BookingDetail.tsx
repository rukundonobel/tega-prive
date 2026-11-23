import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft, MapPin, ArrowRight, Clock, Users, CreditCard,
  CheckCircle2, XCircle, Navigation
} from 'lucide-react'
import { bookingsService } from '@/services/bookings.service'
import { Button, Spinner } from '@/components/ui'
import { StatusBadge } from '@/components/StatusBadge'
function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleString([], {
    weekday: 'long', year: 'numeric', month: 'long',
    day: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}

export default function BookingDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: booking, isLoading } = useQuery({
    queryKey: ['booking', id],
    queryFn: () => bookingsService.myBooking(id!),
    enabled: !!id,
  })

  const cancelMut = useMutation({
    mutationFn: () => bookingsService.cancel(id!, 'Cancelled by passenger'),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['booking', id] })
      qc.invalidateQueries({ queryKey: ['my-bookings'] })
    },
  })

  if (isLoading || !booking) {
    return (
      <div className="grid min-h-screen place-items-center bg-mist">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  const isPending = booking.status === 'PENDING_PAYMENT'
  const isConfirmed = booking.status === 'CONFIRMED'
  const isCancellable = !['CANCELLED', 'COMPLETED', 'REFUNDED'].includes(booking.status)

  return (
    <div className="container-page max-w-lg py-8">
      <button
        onClick={() => navigate('/bookings')}
        className="mb-6 flex items-center gap-2 text-sm text-ink/50 hover:text-forest"
      >
        <ArrowLeft className="h-4 w-4" /> My bookings
      </button>

      <div className="card overflow-hidden">
        {/* Route header */}
        <div className="flex items-center justify-between gap-3 bg-forest px-5 py-5">
          <div className="flex items-center gap-2 font-semibold text-white">
            <MapPin className="h-4 w-4 text-gold shrink-0" />
            {booking.origin}
            <ArrowRight className="h-4 w-4 text-white/40 shrink-0" />
            <MapPin className="h-4 w-4 text-gold shrink-0" />
            {booking.destination}
          </div>
          <StatusBadge status={booking.status} />
        </div>

        <div className="divide-y divide-line p-5">
          <Row icon={<Clock />} label="Departure" value={formatDate(booking.departureTime)} />
          <Row icon={<Users />} label="Seats" value={`${booking.seatsBooked} seat${booking.seatsBooked !== 1 ? 's' : ''}`} />

          <div className="py-4">
            <p className="text-xs uppercase tracking-wide text-ink/40">Fare breakdown</p>
            <div className="mt-2 space-y-1 text-sm">
              <div className="flex justify-between text-ink/70">
                <span>Fare</span>
                <span>{formatRwf(booking.grossFare)}</span>
              </div>
              <div className="flex justify-between text-ink/50">
                <span>Service fee</span>
                <span>{formatRwf(booking.serviceFee)}</span>
              </div>
              <div className="flex justify-between font-semibold text-forest">
                <span>Total</span>
                <span>{formatRwf(booking.totalAmount)}</span>
              </div>
            </div>
          </div>

          {booking.paymentMethod && (
            <Row icon={<CreditCard />} label="Payment" value={booking.paymentMethod.replace('_', ' ')} />
          )}
          {booking.confirmedAt && (
            <Row icon={<CheckCircle2 />} label="Confirmed" value={formatDate(booking.confirmedAt)} />
          )}
          {booking.cancelledAt && (
            <>
              <Row icon={<XCircle />} label="Cancelled" value={formatDate(booking.cancelledAt)} />
              {booking.cancelReason && (
                <p className="py-3 text-sm text-ink/60">{booking.cancelReason}</p>
              )}
            </>
          )}
        </div>

        <div className="flex flex-col gap-2 p-5 pt-0 sm:flex-row">
          {isConfirmed && (
            <Button
              className="flex-1"
              onClick={() => navigate(`/track/${booking.journeyId}`)}
            >
              <Navigation className="h-4 w-4" /> Track vehicle
            </Button>
          )}
          {isPending && (
            <Button
              className="flex-1"
              onClick={() => navigate(`/pay/${id}`)}
            >
              <CreditCard className="h-4 w-4" /> Pay now
            </Button>
          )}
          {isCancellable && (
            <Button
              variant="outline"
              className="flex-1"
              loading={cancelMut.isPending}
              onClick={() => cancelMut.mutate()}
            >
              Cancel booking
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}

function Row({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-3 py-3 text-sm">
      <span className="mt-0.5 text-gold-dark">{icon && <span className="[&>svg]:h-4 [&>svg]:w-4">{icon}</span>}</span>
      <span className="w-24 shrink-0 text-ink/50">{label}</span>
      <span className="text-forest">{value}</span>
    </div>
  )
}
