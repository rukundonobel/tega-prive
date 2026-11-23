import { useState, type FormEvent } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  MapPin, CalendarDays, Users, Car, Star, ArrowRight,
  Wind, Luggage, Clock, ChevronRight, SearchX,
} from 'lucide-react'
import { locationsService, journeysService } from '@/services/journeys.service'
import { Button, Spinner } from '@/components/ui'
import type { Journey } from '@/types'

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' })
}
function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}

function JourneyCard({ journey }: { journey: Journey }) {
  const navigate = useNavigate()
  return (
    <div className="card overflow-hidden transition hover:shadow-card-hover">
      {/* Vehicle photo strip */}
      <div className="relative h-32 bg-mist sm:h-40">
        {journey.vehiclePrimaryPhotoUrl ? (
          <img
            src={journey.vehiclePrimaryPhotoUrl}
            alt={`${journey.vehicleMake} ${journey.vehicleModel}`}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="grid h-full place-items-center text-ink/20">
            <Car className="h-10 w-10" />
          </div>
        )}
        <span className="absolute right-3 top-3 rounded-full bg-white/90 px-2.5 py-1 text-xs font-semibold text-forest">
          {journey.availableSeats} seat{journey.availableSeats !== 1 ? 's' : ''} left
        </span>
      </div>

      <div className="p-4 sm:p-5">
        {/* Route + time */}
        <div className="flex items-center gap-2 text-sm font-medium text-forest">
          <MapPin className="h-3.5 w-3.5 shrink-0 text-gold-dark" />
          {journey.originName}
          <ArrowRight className="h-3.5 w-3.5 shrink-0 text-ink/30" />
          <MapPin className="h-3.5 w-3.5 shrink-0 text-gold-dark" />
          {journey.destinationName}
        </div>

        <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-ink/60">
          <span className="flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" />
            {formatDate(journey.departureTime)} · {formatTime(journey.departureTime)}
          </span>
          {journey.estimatedArrival && (
            <span className="text-ink/40">→ {formatTime(journey.estimatedArrival)}</span>
          )}
        </div>

        {/* Driver + vehicle */}
        <div className="mt-4 flex items-center gap-3">
          <div className="grid h-10 w-10 shrink-0 place-items-center overflow-hidden rounded-full bg-mist text-xs font-semibold text-forest">
            {journey.driverPhotoUrl ? (
              <img src={journey.driverPhotoUrl} alt={journey.driverName} className="h-full w-full object-cover" />
            ) : (
              journey.driverName.charAt(0)
            )}
          </div>
          <div className="min-w-0">
            <p className="truncate font-medium text-forest">{journey.driverName}</p>
            <div className="flex items-center gap-1 text-xs text-ink/50">
              <Star className="h-3 w-3 fill-gold text-gold" />
              {journey.driverRating.toFixed(1)}
              <span className="text-ink/30">·</span>
              {journey.vehicleMake} {journey.vehicleModel}
            </div>
          </div>
        </div>

        {/* Amenities */}
        <div className="mt-3 flex flex-wrap gap-2">
          <span className="flex items-center gap-1 rounded-full bg-mist px-2.5 py-1 text-xs text-ink/60">
            <Users className="h-3.5 w-3.5" /> {journey.vehicleCategory}
          </span>
          {journey.vehicleHasAc && (
            <span className="flex items-center gap-1 rounded-full bg-mist px-2.5 py-1 text-xs text-ink/60">
              <Wind className="h-3.5 w-3.5" /> A/C
            </span>
          )}
          <span className="flex items-center gap-1 rounded-full bg-mist px-2.5 py-1 text-xs text-ink/60">
            <Luggage className="h-3.5 w-3.5" /> Luggage ok
          </span>
        </div>

        {/* Price + CTA */}
        <div className="mt-4 flex items-center justify-between">
          <div>
            <p className="font-display text-xl font-semibold text-forest">
              {formatRwf(journey.pricePerSeat)}
            </p>
            <p className="text-xs text-ink/50">per seat</p>
          </div>
          <Button
            size="sm"
            onClick={() => navigate(`/journeys/${journey.id}`)}
          >
            Book now <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

export default function Search() {
  const [searchParams, setSearchParams] = useSearchParams()

  const [from, setFrom] = useState(searchParams.get('from') ?? '')
  const [to, setTo] = useState(searchParams.get('to') ?? '')
  const [date, setDate] = useState(searchParams.get('date') ?? '')
  const [seats, setSeats] = useState(Number(searchParams.get('passengers') ?? 1))

  const { data: locations } = useQuery({
    queryKey: ['locations'],
    queryFn: locationsService.list,
  })

  const originId = locations?.find((l) => l.name === from)?.id
  const destinationId = locations?.find((l) => l.name === to)?.id

  const { data: results, isLoading, isFetching } = useQuery({
    queryKey: ['journeys', 'search', originId, destinationId, date, seats],
    queryFn: () =>
      journeysService.search({ originId: originId!, destinationId: destinationId!, date, seats }),
    enabled: !!originId && !!destinationId && !!date,
  })

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    const p = new URLSearchParams()
    if (from) p.set('from', from)
    if (to) p.set('to', to)
    if (date) p.set('date', date)
    if (seats !== 1) p.set('passengers', String(seats))
    setSearchParams(p)
  }

  const hasSearched = !!originId && !!destinationId && !!date
  const busy = isLoading || isFetching

  return (
    <div className="container-page py-6 sm:py-10">
      {/* Search form — sticky on mobile so it stays accessible while scrolling */}
      <form
        onSubmit={onSearch}
        className="sticky top-16 z-30 rounded-2xl border border-line bg-white p-4 shadow-card sm:static sm:shadow-none"
      >
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {/* From */}
          <div>
            <label className="label">From</label>
            <div className="flex items-center gap-2 rounded-xl border border-line px-3 focus-within:border-gold focus-within:ring-2 focus-within:ring-gold/30">
              <MapPin className="h-4 w-4 shrink-0 text-gold-dark" />
              <select
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className="h-11 w-full bg-transparent outline-none"
              >
                <option value="">Select city</option>
                {locations?.map((l) => (
                  <option key={l.id} value={l.name}>
                    {l.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* To */}
          <div>
            <label className="label">To</label>
            <div className="flex items-center gap-2 rounded-xl border border-line px-3 focus-within:border-gold focus-within:ring-2 focus-within:ring-gold/30">
              <MapPin className="h-4 w-4 shrink-0 text-gold-dark" />
              <select
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className="h-11 w-full bg-transparent outline-none"
              >
                <option value="">Select city</option>
                {locations?.map((l) => (
                  <option key={l.id} value={l.name}>
                    {l.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Date */}
          <div>
            <label className="label">Date</label>
            <div className="flex items-center gap-2 rounded-xl border border-line px-3 focus-within:border-gold focus-within:ring-2 focus-within:ring-gold/30">
              <CalendarDays className="h-4 w-4 shrink-0 text-gold-dark" />
              <input
                type="date"
                value={date}
                min={new Date().toISOString().split('T')[0]}
                onChange={(e) => setDate(e.target.value)}
                className="h-11 w-full bg-transparent outline-none"
              />
            </div>
          </div>

          {/* Seats */}
          <div>
            <label className="label">Seats</label>
            <div className="flex items-center gap-2 rounded-xl border border-line px-3 focus-within:border-gold focus-within:ring-2 focus-within:ring-gold/30">
              <Users className="h-4 w-4 shrink-0 text-gold-dark" />
              <select
                value={seats}
                onChange={(e) => setSeats(Number(e.target.value))}
                className="h-11 w-full bg-transparent outline-none"
              >
                {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                  <option key={n} value={n}>
                    {n} passenger{n !== 1 ? 's' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <Button type="submit" className="mt-4 w-full sm:w-auto">
          Search rides
        </Button>
      </form>

      {/* Results */}
      <div className="mt-6 sm:mt-8">
        {busy && (
          <div className="grid place-items-center py-20">
            <Spinner className="h-8 w-8" />
          </div>
        )}

        {!busy && hasSearched && results && results.length > 0 && (
          <>
            <p className="mb-4 text-sm text-ink/60">
              {results.length} ride{results.length !== 1 ? 's' : ''} found · {from} → {to}
            </p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {results.map((j) => (
                <JourneyCard key={j.id} journey={j} />
              ))}
            </div>
          </>
        )}

        {!busy && hasSearched && results && results.length === 0 && (
          <div className="card p-10 text-center">
            <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-mist">
              <SearchX className="h-7 w-7 text-gold-dark" />
            </div>
            <p className="mt-5 font-semibold text-forest">No rides found</p>
            <p className="mt-1 text-sm text-ink/60">
              No journeys available from <strong>{from}</strong> to <strong>{to}</strong> on this date.
              Try a different date or fewer seats.
            </p>
          </div>
        )}

        {!hasSearched && (
          <div className="card p-10 text-center">
            <MapPin className="mx-auto h-10 w-10 text-gold/40" />
            <p className="mt-4 text-sm text-ink/50">
              Select a departure, destination and date above to search for available rides.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}
