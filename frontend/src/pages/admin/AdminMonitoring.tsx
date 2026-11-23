import { useQuery } from '@tanstack/react-query'
import { Navigation, MapPin, ArrowRight, Users, Clock, Wifi } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { Spinner } from '@/components/ui'

function formatTime(iso: string) {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function FreshnessIndicator({ lastUpdate }: { lastUpdate?: string }) {
  if (!lastUpdate) return <span className="text-xs text-ink/30">No signal</span>
  const age = Date.now() - new Date(lastUpdate).getTime()
  const fresh = age < 60_000
  return (
    <span className={`flex items-center gap-1 text-xs ${fresh ? 'text-green-600' : 'text-orange-500'}`}>
      <Wifi className="h-3.5 w-3.5" />
      {fresh ? `${Math.round(age / 1000)}s ago` : 'Signal lost'}
    </span>
  )
}

export default function AdminMonitoring() {
  const { data: journeys, isLoading, refetch } = useQuery({
    queryKey: ['admin', 'activeJourneys'],
    queryFn: adminService.activeJourneys,
    refetchInterval: 15_000,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold text-forest">Live monitoring</h1>
          <p className="mt-1 text-sm text-ink/60">
            All active journeys · auto-refreshes every 15 seconds
          </p>
        </div>
        <button onClick={() => refetch()} className="text-sm text-gold-dark hover:text-gold">
          Refresh now
        </button>
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      {journeys && journeys.length === 0 && (
        <div className="card p-16 text-center">
          <Navigation className="mx-auto h-12 w-12 text-ink/20" />
          <p className="mt-4 font-medium text-forest">No active journeys</p>
          <p className="mt-1 text-sm text-ink/50">Journey tracking will appear here when drivers are on the road.</p>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {(journeys ?? []).map((j) => (
          <div key={j.journeyId} className="card overflow-hidden">
            {/* Map preview — shows OSM if coords available */}
            {j.latitude && j.longitude ? (
              <div className="relative h-32 overflow-hidden bg-mist">
                <iframe
                  title={`Journey ${j.journeyId}`}
                  className="h-full w-full border-0"
                  src={`https://www.openstreetmap.org/export/embed.html?bbox=${j.longitude - 0.01},${j.latitude - 0.01},${j.longitude + 0.01},${j.latitude + 0.01}&layer=mapnik&marker=${j.latitude},${j.longitude}`}
                  loading="lazy"
                />
                <div className="absolute inset-0 pointer-events-none" />
              </div>
            ) : (
              <div className="grid h-32 place-items-center bg-forest/5">
                <MapPin className="h-8 w-8 text-forest/20" />
              </div>
            )}

            <div className="p-4 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1 text-sm font-medium text-forest truncate">
                  <MapPin className="h-3.5 w-3.5 text-gold-dark shrink-0" />
                  {j.origin}
                  <ArrowRight className="h-3.5 w-3.5 text-ink/30 shrink-0" />
                  {j.destination}
                </div>
                <span className="ml-2 flex items-center gap-1 rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700 shrink-0">
                  <span className="h-1.5 w-1.5 rounded-full bg-green-600 animate-pulse" />
                  Live
                </span>
              </div>

              <p className="text-sm text-ink/70">{j.driverName}</p>
              <p className="text-xs text-ink/50">{j.vehicleName} · {j.registrationNumber}</p>

              <div className="flex items-center justify-between pt-1">
                <span className="flex items-center gap-1 text-xs text-ink/50">
                  <Users className="h-3.5 w-3.5" />
                  {j.bookedSeats}/{j.totalSeats} seats
                </span>
                <span className="flex items-center gap-1 text-xs text-ink/50">
                  <Clock className="h-3.5 w-3.5" />
                  Dep. {formatTime(j.departureTime)}
                </span>
                <FreshnessIndicator lastUpdate={j.lastGpsUpdate ?? undefined} />
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
