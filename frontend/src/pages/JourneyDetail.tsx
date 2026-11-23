import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import {
  ArrowLeft, MapPin, Clock, Users, Car, Wind, Star, Luggage,
  ArrowRight, CheckCircle2, ChevronRight
} from 'lucide-react'
import { journeysService } from '@/services/journeys.service'
import { bookingsService } from '@/services/bookings.service'
import { Button, Spinner } from '@/components/ui'
import { useAuth } from '@/context/AuthContext'
import type { ApiError } from '@/types'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}
function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}

export default function JourneyDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [seats, setSeats] = useState(1)
  const [bookingId, setBookingId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { data: journey, isLoading } = useQuery({
    queryKey: ['journey', id],
    queryFn: () => journeysService.get(id!),
    enabled: !!id,
  })

  const bookMut = useMutation({
    mutationFn: () => bookingsService.create(id!, seats),
    onSuccess: (booking) => setBookingId(booking.id),
    onError: (e) => setError((e as ApiError).message),
  })

  if (isLoading || !journey) {
    return (
      <div className="grid min-h-screen place-items-center bg-mist">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  const serviceFee = journey.pricePerSeat * seats * 0.03
  const total = journey.pricePerSeat * seats + serviceFee

  if (bookingId) {
    return (
      <div className="container-page flex min-h-screen flex-col items-center justify-center py-20">
        <div className="card w-full max-w-sm p-8 text-center">
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-forest">
            <CheckCircle2 className="h-8 w-8 text-gold" />
          </div>
          <h1 className="mt-5 font-display text-xl font-semibold text-forest">Booking created!</h1>
          <p className="mt-2 text-sm text-ink/60">
            Your booking is pending payment. Complete payment to confirm your seat.
          </p>
          <div className="mt-6 space-y-2">
            <Button className="w-full" onClick={() => navigate(`/bookings/${bookingId}`)}>
              View booking & pay
            </Button>
            <Button variant="ghost" className="w-full" onClick={() => navigate('/bookings')}>
              My bookings
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-mist pb-32 sm:pb-10">
      {/* Vehicle photo hero */}
      <div className="relative h-48 w-full bg-forest sm:h-64">
        {journey.vehiclePrimaryPhotoUrl ? (
          <img
            src={journey.vehiclePrimaryPhotoUrl}
            alt={`${journey.vehicleMake} ${journey.vehicleModel}`}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="grid h-full place-items-center text-white/20">
            <Car className="h-14 w-14" />
          </div>
        )}
        <button
          onClick={() => navigate(-1)}
          className="absolute left-4 top-4 grid h-10 w-10 place-items-center rounded-full bg-black/40 text-white backdrop-blur"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
      </div>

      <div className="container-page max-w-2xl -mt-6">
        <div className="card p-5 sm:p-6">
          {/* Route */}
          <div className="flex items-center gap-2 font-semibold text-forest">
            <MapPin className="h-4 w-4 text-gold-dark" />
            {journey.originName}
            <ArrowRight className="h-4 w-4 text-ink/30" />
            <MapPin className="h-4 w-4 text-gold-dark" />
            {journey.destinationName}
          </div>

          {/* Date & time */}
          <div className="mt-3 flex flex-wrap gap-4 text-sm text-ink/60">
            <span className="flex items-center gap-1.5">
              <Clock className="h-4 w-4" />
              {formatDate(journey.departureTime)} at {formatTime(journey.departureTime)}
            </span>
            {journey.estimatedArrival && (
              <span>Estimated arrival: {formatTime(journey.estimatedArrival)}</span>
            )}
          </div>

          {/* Driver */}
          <div className="mt-5 flex items-center gap-3 border-t border-line pt-4">
            <div className="grid h-12 w-12 shrink-0 place-items-center overflow-hidden rounded-full bg-mist text-sm font-semibold text-forest">
              {journey.driverPhotoUrl ? (
                <img src={journey.driverPhotoUrl} alt={journey.driverName} className="h-full w-full object-cover" />
              ) : (
                journey.driverName.charAt(0)
              )}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <p className="font-semibold text-forest">{journey.driverName}</p>
                {journey.driverVerified && (
                  <span className="rounded-full bg-forest/10 px-2 py-0.5 text-xs font-medium text-forest">Verified</span>
                )}
              </div>
              <div className="flex items-center gap-1 text-sm text-ink/50">
                <Star className="h-3.5 w-3.5 fill-gold text-gold" />
                {journey.driverRating.toFixed(1)} · {journey.driverRatingCount} reviews
              </div>
            </div>
          </div>

          {/* Vehicle specs */}
          <div className="mt-4 flex flex-wrap gap-2">
            <Chip icon={<Car className="h-3.5 w-3.5" />} label={`${journey.vehicleMake} ${journey.vehicleModel}`} />
            <Chip icon={<Users className="h-3.5 w-3.5" />} label={journey.vehicleCategory} />
            {journey.vehicleHasAc && <Chip icon={<Wind className="h-3.5 w-3.5" />} label="A/C" />}
            <Chip icon={<Luggage className="h-3.5 w-3.5" />} label="Luggage ok" />
          </div>

          {/* Instructions */}
          {(journey.pickupInstructions || journey.dropoffInstructions || journey.notes) && (
            <div className="mt-5 space-y-2 rounded-xl bg-mist p-4 text-sm text-ink/70">
              {journey.pickupInstructions && <p><strong>Pickup:</strong> {journey.pickupInstructions}</p>}
              {journey.dropoffInstructions && <p><strong>Dropoff:</strong> {journey.dropoffInstructions}</p>}
              {journey.notes && <p><strong>Notes:</strong> {journey.notes}</p>}
            </div>
          )}
        </div>

        {/* Booking panel */}
        <div className="card mt-4 p-5 sm:p-6">
          <h2 className="font-semibold text-forest">Select seats</h2>
          <div className="mt-4 flex items-center gap-4">
            <button
              className="grid h-10 w-10 place-items-center rounded-xl border border-line text-lg font-semibold text-forest hover:bg-mist active:scale-95"
              onClick={() => setSeats((s) => Math.max(1, s - 1))}
            >
              −
            </button>
            <span className="font-display text-2xl font-semibold text-forest w-8 text-center">{seats}</span>
            <button
              className="grid h-10 w-10 place-items-center rounded-xl border border-line text-lg font-semibold text-forest hover:bg-mist active:scale-95"
              onClick={() => setSeats((s) => Math.min(journey.availableSeats, s + 1))}
            >
              +
            </button>
            <span className="text-sm text-ink/50">{journey.availableSeats} available</span>
          </div>

          <div className="mt-5 space-y-1.5 rounded-xl bg-mist p-4 text-sm">
            <Row label="Seat price" value={formatRwf(journey.pricePerSeat)} />
            <Row label={`× ${seats} seat${seats !== 1 ? 's' : ''}`} value={formatRwf(journey.pricePerSeat * seats)} />
            <Row label="Service fee (3%)" value={formatRwf(serviceFee)} muted />
            <div className="border-t border-line pt-2">
              <Row label="Total" value={formatRwf(total)} bold />
            </div>
          </div>

          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        </div>
      </div>

      {/* Mobile sticky CTA */}
      <div className="fixed bottom-0 left-0 right-0 border-t border-line bg-white px-4 py-3 sm:static sm:mt-4 sm:border-0 sm:bg-transparent sm:px-0 sm:py-0">
        <div className="container-page max-w-2xl sm:px-0">
          {user ? (
            <Button
              className="w-full text-base"
              size="lg"
              loading={bookMut.isPending}
              onClick={() => bookMut.mutate()}
              disabled={journey.availableSeats === 0}
            >
              {journey.availableSeats === 0 ? 'Fully booked' : `Book ${seats} seat${seats !== 1 ? 's' : ''} · ${formatRwf(total)}`}
              {journey.availableSeats > 0 && <ChevronRight className="h-5 w-5" />}
            </Button>
          ) : (
            <Button
              className="w-full text-base"
              size="lg"
              onClick={() => navigate('/login', { state: { from: `/journeys/${id}` } })}
            >
              Sign in to book
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}

function Chip({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <span className="flex items-center gap-1 rounded-full bg-mist px-2.5 py-1 text-xs text-ink/60">
      {icon} {label}
    </span>
  )
}

function Row({ label, value, muted = false, bold = false }: {
  label: string; value: string; muted?: boolean; bold?: boolean
}) {
  return (
    <div className={`flex justify-between ${muted ? 'text-ink/50' : ''} ${bold ? 'font-semibold text-forest' : ''}`}>
      <span>{label}</span>
      <span>{value}</span>
    </div>
  )
}
