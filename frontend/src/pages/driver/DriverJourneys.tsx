import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Plus, MapPin, ArrowRight, Clock, Users, Play, CheckCheck,
  XCircle, Loader2, ChevronRight, Navigation
} from 'lucide-react'
import { journeysService } from '@/services/journeys.service'
import { bookingsService } from '@/services/bookings.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Spinner } from '@/components/ui'
import type { Journey, ApiError } from '@/types'

function formatDate(iso: string) {
  return new Date(iso).toLocaleString([], {
    weekday: 'short', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}
function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}

function JourneyRow({ journey, onAction }: {
  journey: Journey
  onAction: (action: 'start' | 'complete' | 'cancel', id: string) => void
}) {
  const [expanded, setExpanded] = useState(false)
  const navigate = useNavigate()
  const { data: bookings } = useQuery({
    queryKey: ['journey-bookings', journey.id],
    queryFn: () => bookingsService.journeyBookings(journey.id),
    enabled: expanded,
  })

  return (
    <div className="card overflow-hidden">
      <button
        className="flex w-full items-center gap-3 p-5 text-left"
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="flex items-center gap-1 font-semibold text-forest">
              <MapPin className="h-4 w-4 text-gold-dark shrink-0" />
              {journey.originName}
              <ArrowRight className="h-4 w-4 text-ink/30 shrink-0" />
              {journey.destinationName}
            </span>
            <StatusBadge status={journey.status} />
          </div>
          <div className="mt-1.5 flex flex-wrap gap-3 text-xs text-ink/60">
            <span className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" /> {formatDate(journey.departureTime)}
            </span>
            <span className="flex items-center gap-1">
              <Users className="h-3.5 w-3.5" />
              {journey.totalSeats - journey.availableSeats}/{journey.totalSeats} booked
            </span>
            <span>{formatRwf(journey.pricePerSeat)} / seat</span>
          </div>
        </div>
        <ChevronRight className={`h-5 w-5 shrink-0 text-ink/30 transition ${expanded ? 'rotate-90' : ''}`} />
      </button>

      {/* Actions row */}
      <div className="flex flex-wrap gap-2 border-t border-line px-5 py-3">
        {journey.status === 'SCHEDULED' && (
          <>
            <Button size="sm" onClick={() => onAction('start', journey.id)}>
              <Play className="h-4 w-4" /> Start
            </Button>
            <Button size="sm" variant="outline" onClick={() => onAction('cancel', journey.id)}>
              <XCircle className="h-4 w-4" /> Cancel
            </Button>
          </>
        )}
        {journey.status === 'ACTIVE' && (
          <>
            <Button size="sm" onClick={() => navigate(`/driver/journeys/${journey.id}/track`)}>
              <Navigation className="h-4 w-4" /> Track
            </Button>
            <Button size="sm" variant="outline" onClick={() => onAction('complete', journey.id)}>
              <CheckCheck className="h-4 w-4" /> Complete
            </Button>
          </>
        )}
      </div>

      {/* Expanded: passenger bookings */}
      {expanded && (
        <div className="border-t border-line bg-mist px-5 py-4">
          {!bookings && <Loader2 className="h-4 w-4 animate-spin text-ink/40" />}
          {bookings && bookings.length === 0 && (
            <p className="text-sm text-ink/50">No bookings yet.</p>
          )}
          {bookings?.map((b) => (
            <div key={b.id} className="flex items-center justify-between py-2 text-sm">
              <div>
                <p className="font-medium text-forest">{b.passengerName}</p>
                <p className="text-xs text-ink/50">
                  {b.seatsBooked} seat{b.seatsBooked !== 1 ? 's' : ''} · {b.paymentMethod ?? 'Pending payment'}
                </p>
              </div>
              <StatusBadge status={b.status} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default function DriverJourneys() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [error, setError] = useState<string | null>(null)

  const { data: journeys, isLoading } = useQuery({
    queryKey: ['my-journeys'],
    queryFn: journeysService.myJourneys,
  })

  const startMut = useMutation({
    mutationFn: (id: string) => journeysService.start(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-journeys'] }),
    onError: (e) => setError((e as ApiError).message),
  })
  const completeMut = useMutation({
    mutationFn: (id: string) => journeysService.complete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-journeys'] }),
    onError: (e) => setError((e as ApiError).message),
  })
  const cancelMut = useMutation({
    mutationFn: (id: string) => journeysService.cancel(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-journeys'] }),
    onError: (e) => setError((e as ApiError).message),
  })

  const handleAction = (action: 'start' | 'complete' | 'cancel', id: string) => {
    setError(null)
    if (action === 'start') startMut.mutate(id)
    if (action === 'complete') completeMut.mutate(id)
    if (action === 'cancel') cancelMut.mutate(id)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold text-forest">My journeys</h1>
          <p className="mt-1 text-sm text-ink/60">Publish journeys and manage passenger bookings.</p>
        </div>
        <Button onClick={() => navigate('/driver/journeys/new')}>
          <Plus className="h-4 w-4" /> Publish journey
        </Button>
      </div>

      {error && (
        <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {isLoading && (
        <div className="grid place-items-center py-16"><Spinner /></div>
      )}

      <div className="space-y-3">
        {(journeys ?? []).map((j) => (
          <JourneyRow key={j.id} journey={j} onAction={handleAction} />
        ))}
        {journeys && journeys.length === 0 && (
          <div className="card p-12 text-center">
            <MapPin className="mx-auto h-10 w-10 text-ink/20" />
            <p className="mt-4 font-medium text-forest">No journeys yet</p>
            <p className="mt-1 text-sm text-ink/50">Publish your first journey to start accepting bookings.</p>
            <div className="mt-5">
              <Button onClick={() => navigate('/driver/journeys/new')}>
                <Plus className="h-4 w-4" /> Publish journey
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
