import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft, Navigation, Wifi, WifiOff, AlertTriangle, Loader2,
  MapPin, ArrowRight, Clock
} from 'lucide-react'
import { trackingService } from '@/services/tracking.service'
import { journeysService } from '@/services/journeys.service'
import { usePassengerTracking } from '@/hooks/useTracking'

const GOOGLE_MAPS_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

function formatTime(ts: number) {
  if (!ts) return '—'
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

/** Simple OpenStreetMap iframe shown when Google Maps is not configured. */
function OsmFallback({ lat, lon }: { lat: number; lon: number }) {
  const bbox = `${lon - 0.02},${lat - 0.02},${lon + 0.02},${lat + 0.02}`
  return (
    <iframe
      title="Vehicle location"
      className="h-full w-full rounded-2xl border-0"
      src={`https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat},${lon}`}
      loading="lazy"
    />
  )
}

/** Google Maps iframe embed (no SDK, no API key billing for simple embeds). */
function GoogleMapsEmbed({ lat, lon, apiKey }: { lat: number; lon: number; apiKey: string }) {
  const src = `https://www.google.com/maps/embed/v1/view?key=${apiKey}&center=${lat},${lon}&zoom=14&maptype=roadmap`
  return (
    <iframe
      title="Vehicle location"
      className="h-full w-full rounded-2xl border-0"
      src={src}
      loading="lazy"
      referrerPolicy="no-referrer-when-downgrade"
      allowFullScreen
    />
  )
}

function MapArea({ lat, lon }: { lat: number; lon: number }) {
  if (GOOGLE_MAPS_KEY) {
    return <GoogleMapsEmbed lat={lat} lon={lon} apiKey={GOOGLE_MAPS_KEY} />
  }
  return <OsmFallback lat={lat} lon={lon} />
}

function NoLocationPlaceholder() {
  return (
    <div className="grid h-full place-items-center bg-forest/5 rounded-2xl">
      <div className="text-center px-6">
        <Navigation className="mx-auto h-12 w-12 text-forest/20" />
        <p className="mt-3 font-medium text-forest">Waiting for driver's location…</p>
        <p className="mt-1 text-sm text-ink/50">
          The map will appear once the driver starts sharing their position.
        </p>
      </div>
    </div>
  )
}

export default function TrackJourney() {
  const { journeyId } = useParams<{ journeyId: string }>()
  const navigate = useNavigate()

  const { data: journey } = useQuery({
    queryKey: ['journey', journeyId],
    queryFn: () => journeysService.get(journeyId!),
    enabled: !!journeyId,
  })

  const { data: initialState } = useQuery({
    queryKey: ['tracking-state', journeyId],
    queryFn: () => trackingService.getState(journeyId!),
    enabled: !!journeyId,
    refetchInterval: 10_000,  // poll every 10s as a fallback if WS drops
  })

  const { fix: wsFix, fresh, connected, error: wsError } = usePassengerTracking(journeyId ?? null)

  // Use WebSocket fix if available, fall back to REST snapshot
  const lat = wsFix?.latitude ?? initialState?.latitude ?? null
  const lon = wsFix?.longitude ?? initialState?.longitude ?? null
  const lastUpdated = wsFix?.recordedAt ?? initialState?.lastUpdatedAt ?? 0

  const journeyStatus = initialState?.journeyStatus ?? journey?.status ?? 'SCHEDULED'

  return (
    <div className="flex min-h-screen flex-col bg-mist">
      {/* Header */}
      <div className="sticky top-0 z-10 border-b border-line bg-white px-4 py-3">
        <div className="container-page flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="grid h-10 w-10 shrink-0 place-items-center rounded-xl hover:bg-mist"
          >
            <ArrowLeft className="h-5 w-5 text-forest" />
          </button>

          {journey ? (
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 font-semibold text-forest text-sm truncate">
                <MapPin className="h-4 w-4 text-gold-dark shrink-0" />
                {journey.originName}
                <ArrowRight className="h-4 w-4 text-ink/30 shrink-0" />
                <MapPin className="h-4 w-4 text-gold-dark shrink-0" />
                {journey.destinationName}
              </div>
              <div className="mt-0.5 flex items-center gap-1 text-xs text-ink/50">
                <Clock className="h-3.5 w-3.5" />
                Departed {new Date(journey.departureTime).toLocaleTimeString([], {
                  hour: '2-digit', minute: '2-digit',
                })}
              </div>
            </div>
          ) : (
            <div className="flex-1" />
          )}

          {/* Connection status pill */}
          <div className={`flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${
            connected ? 'bg-green-100 text-green-700' : 'bg-gold/20 text-gold-dark'
          }`}>
            {connected
              ? <><Wifi className="h-3.5 w-3.5" /> Live</>
              : <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Connecting</>
            }
          </div>
        </div>
      </div>

      {/* Map — full remaining height */}
      <div className="relative flex-1 p-4">
        <div className="h-full min-h-[55vh] overflow-hidden rounded-2xl shadow-card">
          {lat !== null && lon !== null
            ? <MapArea lat={lat} lon={lon} />
            : <NoLocationPlaceholder />
          }
        </div>

        {/* Freshness overlay — shown when signal lost */}
        {lat !== null && !fresh && wsFix !== null && (
          <div className="absolute inset-x-4 bottom-6 flex items-center justify-center">
            <div className="flex items-center gap-2 rounded-xl bg-white/90 px-4 py-2.5 shadow-card backdrop-blur text-sm font-medium text-orange-700">
              <WifiOff className="h-4 w-4" />
              GPS signal lost · Last update: {formatTime(lastUpdated)}
            </div>
          </div>
        )}
      </div>

      {/* Stats strip */}
      <div className="border-t border-line bg-white px-4 py-4 pb-safe">
        <div className="container-page grid grid-cols-3 gap-4">
          <Stat
            label="Status"
            value={journeyStatus === 'ACTIVE' ? 'On the way' : journeyStatus.toLowerCase()}
          />
          <Stat
            label="Last update"
            value={lastUpdated ? formatTime(lastUpdated) : 'No signal'}
          />
          <Stat
            label="Speed"
            value={wsFix?.speed ? `${Math.round(wsFix.speed)} km/h` : '—'}
          />
        </div>

        {wsError && (
          <div className="mt-3 flex items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            {wsError}
          </div>
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-center">
      <p className="text-xs uppercase tracking-wide text-ink/40">{label}</p>
      <p className="mt-0.5 font-medium text-forest">{value}</p>
    </div>
  )
}
