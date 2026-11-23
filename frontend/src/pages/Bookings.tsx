import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  MapPin, ArrowRight, Clock, CheckCircle2, XCircle,
  CreditCard, ChevronRight, AlertTriangle
} from 'lucide-react'
import { bookingsService } from '@/services/bookings.service'
import { Button, Spinner } from '@/components/ui'
import { StatusBadge } from '@/components/StatusBadge'
import type { Booking, ApiError } from '@/types'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleString([], {
    weekday: 'short', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function BookingCard({ booking, onConfirm, onCancel }: {
  booking: Booking
  onConfirm: (id: string) => void
  onCancel: (id: string) => void
}) {
  const isPending = booking.status === 'PENDING_PAYMENT'
  const isCancellable = !['CANCELLED', 'COMPLETED', 'REFUNDED'].includes(booking.status)

  return (
    <div className="card overflow-hidden">
      {/* Route header */}
      <div className="flex items-center gap-2 bg-forest px-5 py-4 text-sm font-semibold text-white">
        <MapPin className="h-4 w-4 text-gold shrink-0" />
        {booking.origin}
        <ArrowRight className="h-4 w-4 text-white/40 shrink-0" />
        <MapPin className="h-4 w-4 text-gold shrink-0" />
        {booking.destination}
        <div className="ml-auto">
          <StatusBadge status={booking.status} />
        </div>
      </div>

      <div className="p-5">
        <div className="flex flex-wrap items-center gap-4 text-sm text-ink/60">
          <span className="flex items-center gap-1.5">
            <Clock className="h-4 w-4" />
            {formatDate(booking.departureTime)}
          </span>
          <span>{booking.seatsBooked} seat{booking.seatsBooked !== 1 ? 's' : ''}</span>
        </div>

        <div className="mt-4 flex items-end justify-between">
          <div>
            <p className="text-xs text-ink/40">Total</p>
            <p className="font-display text-xl font-semibold text-forest">
              {formatRwf(booking.totalAmount)}
            </p>
          </div>

          <div className="flex gap-2">
            {isPending && (
              <Button size="sm" onClick={() => onConfirm(booking.id)}>
                <CreditCard className="h-4 w-4" /> Pay now
              </Button>
            )}            {isCancellable && !isPending && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => onCancel(booking.id)}
              >
                Cancel
              </Button>
            )}
            <Link to={`/bookings/${booking.id}`}>
              <Button size="sm" variant="ghost">
                Details <ChevronRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default function Bookings() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [cancellingId, setCancellingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data: bookings, isLoading } = useQuery({
    queryKey: ['my-bookings'],
    queryFn: bookingsService.myBookings,
  })

  const cancelMut = useMutation({
    mutationFn: (id: string) => bookingsService.cancel(id, 'Cancelled by passenger'),
    onSuccess: () => {
      setCancellingId(null)
      qc.invalidateQueries({ queryKey: ['my-bookings'] })
    },
    onError: (e) => setError((e as ApiError).message),
  })

  return (
    <div className="container-page py-8 sm:py-12">
      <div className="mb-6">
        <h1 className="font-display text-2xl font-semibold text-forest">My bookings</h1>
        <p className="mt-1 text-sm text-ink/60">Track and manage your travel bookings.</p>
      </div>

      {error && (
        <div className="mb-4 flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          <AlertTriangle className="h-4 w-4 shrink-0" /> {error}
        </div>
      )}

      {isLoading && (
        <div className="grid place-items-center py-20">
          <Spinner />
        </div>
      )}

      {/* Cancel confirmation sheet */}
      {cancellingId && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center">
          <div className="w-full max-w-sm rounded-t-2xl bg-white p-6 sm:rounded-2xl">
            <XCircle className="mx-auto h-10 w-10 text-red-500" />
            <p className="mt-4 text-center font-semibold text-forest">Cancel this booking?</p>
            <p className="mt-1 text-center text-sm text-ink/60">
              The seats will be released. Refund rules apply.
            </p>
            <div className="mt-6 flex gap-3">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setCancellingId(null)}
              >
                Keep booking
              </Button>
              <Button
                variant="danger"
                className="flex-1"
                loading={cancelMut.isPending}
                onClick={() => cancelMut.mutate(cancellingId)}
              >
                Cancel booking
              </Button>
            </div>
          </div>
        </div>
      )}

      <div className="space-y-4">
        {(bookings ?? []).map((b) => (
          <BookingCard
            key={b.id}
            booking={b}
            onConfirm={(id) => navigate(`/pay/${id}`)}
            onCancel={(id) => setCancellingId(id)}
          />
        ))}

        {bookings && bookings.length === 0 && (
          <div className="card p-12 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-ink/20" />
            <p className="mt-4 font-medium text-forest">No bookings yet</p>
            <p className="mt-1 text-sm text-ink/50">Search for a ride and book your first journey.</p>
            <Link to="/search" className="mt-5 inline-block">
              <Button>Find a ride</Button>
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}
