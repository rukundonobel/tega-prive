import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Navigation, Play, AlertTriangle, CheckCircle2,
  Wifi, WifiOff, ArrowLeft, Clock, MapPin, ArrowRight
} from 'lucide-react'
import { journeysService } from '@/services/journeys.service'
import { useDriverTracking } from '@/hooks/useTracking'
import { Button } from '@/components/ui'
import type { ApiError } from '@/types'
import { useMutation, useQueryClient } from '@tanstack/react-query'

function formatTime(ms: number) {
  return new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export default function DriverTracking() {
  const { journeyId } = useParams<{ journeyId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [trackError, setTrackError] = useState<string | null>(null)

  const { data: journey } = useQuery({
    queryKey: ['journey', journeyId],
    queryFn: () => journeysService.get(journeyId!),
    enabled: !!journeyId,
    refetchInterval: 10_000,
  })

  const { start, stop, sending, lastSent, error: gpsError } = useDriverTracking(
    journey?.status === 'ACTIVE' ? (journeyId ?? null) : null
  )

  const startMut = useMutation({
    mutationFn: () => journeysService.start(journeyId!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['journey', journeyId] })
      qc.invalidateQueries({ queryKey: ['my-journeys'] })
      start()
    },
    onError: (e) => setTrackError((e as ApiError).message),
  })

  const completeMut = useMutation({
    mutationFn: () => journeysService.complete(journeyId!),
    onSuccess: () => {
      stop()
      qc.invalidateQueries({ queryKey: ['journey', journeyId] })
      qc.invalidateQueries({ queryKey: ['my-journeys'] })
      navigate('/driver/journeys')
    },
    onError: (e) => setTrackError((e as ApiError).message),
  })

  // Auto-start tracking if journey is already active on mount
  useEffect(() => {
    if (journey?.status === 'ACTIVE' && !sending) {
      start()
    }
  }, [journey?.status])

  const isScheduled = journey?.status === 'SCHEDULED'
  const isActive = journey?.status === 'ACTIVE'
  const isCompleted = journey?.status === 'COMPLETED'

  return (
    <div className="max-w-lg space-y-5">
      <button
        onClick={() => navigate('/driver/journeys')}
        className="flex items-center gap-2 text-sm text-ink/50 hover:text-forest"
      >
        <ArrowLeft className="h-4 w-4" /> My journeys
      </button>

      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Journey tracking</h1>
        <p className="mt-1 text-sm text-ink/60">
          Share your location live so passengers can track the journey.
        </p>
      </div>

      {/* Journey info */}
      {journey && (
        <div className="card p-5">
          <div className="flex items-center gap-2 font-semibold text-forest">
            <MapPin className="h-4 w-4 text-gold-dark" />
            {journey.originName}
            <ArrowRight className="h-4 w-4 text-ink/30" />
            <MapPin className="h-4 w-4 text-gold-dark" />
            {journey.destinationName}
          </div>
          <div className="mt-2 flex items-center gap-2 text-sm text-ink/50">
            <Clock className="h-4 w-4" />
            {new Date(journey.departureTime).toLocaleString([], {
              weekday: 'short', month: 'short', day: 'numeric',
              hour: '2-digit', minute: '2-digit',
            })}
          </div>
          <div className="mt-3">
            <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${
              isActive ? 'bg-green-100 text-green-700' :
              isCompleted ? 'bg-forest/10 text-forest' :
              'bg-gold/20 text-gold-dark'
            }`}>
              {isActive && <span className="h-2 w-2 rounded-full bg-green-600 animate-pulse" />}
              {journey.status.toLowerCase().replace('_', ' ')}
            </span>
          </div>
        </div>
      )}

      {/* GPS status card */}
      <div className="card p-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`grid h-11 w-11 place-items-center rounded-xl ${
              sending ? 'bg-green-100' : 'bg-mist'
            }`}>
              {sending
                ? <Navigation className="h-5 w-5 text-green-600 animate-pulse" />
                : <Navigation className="h-5 w-5 text-ink/30" />
              }
            </div>
            <div>
              <p className="font-medium text-forest">
                {sending ? 'Broadcasting location' : 'Location sharing off'}
              </p>
              <p className="text-xs text-ink/50">
                {lastSent
                  ? `Last sent: ${formatTime(lastSent)}`
                  : 'Start the journey to share your location'}
              </p>
            </div>
          </div>
          {sending
            ? <Wifi className="h-5 w-5 text-green-600" />
            : <WifiOff className="h-5 w-5 text-ink/30" />
          }
        </div>

        {(gpsError || trackError) && (
          <div className="mt-3 flex items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            {gpsError ?? trackError}
          </div>
        )}
      </div>

      {/* Important note about mobile */}
      {isActive && (
        <div className="rounded-xl bg-gold/10 px-4 py-3 text-sm text-gold-dark">
          <strong>Keep this page open</strong> while driving. Some browsers pause location updates when
          the phone is locked — the React Native driver app (coming soon) provides reliable background tracking.
        </div>
      )}

      {/* Actions */}
      <div className="flex flex-col gap-3">
        {isScheduled && (
          <Button
            size="lg"
            className="w-full text-base"
            loading={startMut.isPending}
            onClick={() => startMut.mutate()}
          >
            <Play className="h-5 w-5" /> Start journey & share location
          </Button>
        )}

        {isActive && (
          <>
            {!sending && (
              <Button size="lg" className="w-full" onClick={start}>
                <Navigation className="h-5 w-5" /> Resume location sharing
              </Button>
            )}
            {sending && (
              <Button size="lg" variant="outline" className="w-full" onClick={stop}>
                <WifiOff className="h-5 w-5" /> Pause location sharing
              </Button>
            )}
            <Button
              size="lg"
              variant="dark"
              className="w-full"
              loading={completeMut.isPending}
              onClick={() => {
                stop()
                completeMut.mutate()
              }}
            >
              <CheckCircle2 className="h-5 w-5" /> Complete journey
            </Button>
          </>
        )}

        {isCompleted && (
          <div className="flex items-center justify-center gap-2 rounded-xl bg-forest/10 p-4 text-forest">
            <CheckCircle2 className="h-5 w-5" />
            Journey completed. Well done!
          </div>
        )}
      </div>
    </div>
  )
}
