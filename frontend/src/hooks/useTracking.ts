import { useCallback, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const ACCESS_TOKEN_KEY = 'tp_access_token'
const FRESHNESS_THRESHOLD_MS = 60_000  // 60 seconds

export interface LocationFix {
  journeyId: string
  latitude: number
  longitude: number
  heading?: number
  speed?: number
  accuracy?: number
  device?: string
  recordedAt: number  // epoch-ms
}

export interface TrackingState {
  fix: LocationFix | null
  fresh: boolean        // false if no update received in FRESHNESS_THRESHOLD_MS
  connected: boolean
  error: string | null
}

/**
 * Passenger: subscribes to a journey's GPS topic and returns live location updates.
 * @param journeyId — UUID string of the journey to track
 */
export function usePassengerTracking(journeyId: string | null): TrackingState {
  const [state, setState] = useState<TrackingState>({
    fix: null, fresh: false, connected: false, error: null,
  })
  const clientRef = useRef<Client | null>(null)
  const freshnessTimer = useRef<ReturnType<typeof setTimeout>>()

  useEffect(() => {
    if (!journeyId) return

    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (!token) {
      setState((s) => ({ ...s, error: 'Not authenticated' }))
      return
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL
      ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
      : ''

    const client = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setState((s) => ({ ...s, connected: true, error: null }))
        client.subscribe(`/topic/journey/${journeyId}/location`, (msg) => {
          try {
            const fix: LocationFix = JSON.parse(msg.body)
            clearTimeout(freshnessTimer.current)
            setState((s) => ({ ...s, fix, fresh: true, error: null }))
            // Mark stale if no update arrives within threshold
            freshnessTimer.current = setTimeout(() => {
              setState((s) => ({ ...s, fresh: false }))
            }, FRESHNESS_THRESHOLD_MS)
          } catch {
            /* ignore parse errors */
          }
        })
      },
      onDisconnect: () => setState((s) => ({ ...s, connected: false })),
      onStompError: (frame) =>
        setState((s) => ({ ...s, error: frame.headers?.message ?? 'Connection error' })),
    })

    clientRef.current = client
    client.activate()

    return () => {
      clearTimeout(freshnessTimer.current)
      client.deactivate()
    }
  }, [journeyId])

  return state
}

/**
 * Driver: captures browser geolocation and sends fixes to the backend via STOMP.
 * @param journeyId — UUID string of the ACTIVE journey
 */
export function useDriverTracking(journeyId: string | null) {
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastSent, setLastSent] = useState<number | null>(null)
  const clientRef = useRef<Client | null>(null)
  const watchRef = useRef<number | null>(null)
  const connectedRef = useRef(false)

  const stop = useCallback(() => {
    if (watchRef.current !== null) {
      navigator.geolocation.clearWatch(watchRef.current)
      watchRef.current = null
    }
    clientRef.current?.deactivate()
    connectedRef.current = false
    setSending(false)
  }, [])

  const start = useCallback(() => {
    if (!journeyId) { setError('No journey ID'); return }
    if (!navigator.geolocation) { setError('Geolocation not available'); return }

    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (!token) { setError('Not authenticated'); return }

    const baseUrl = import.meta.env.VITE_API_BASE_URL
      ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
      : ''

    const client = new Client({
      webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connectedRef.current = true
        setSending(true)
        setError(null)

        // Start watching position after STOMP connects
        watchRef.current = navigator.geolocation.watchPosition(
          (pos) => {
            if (!connectedRef.current) return
            const payload = {
              journeyId,
              latitude: pos.coords.latitude,
              longitude: pos.coords.longitude,
              heading: pos.coords.heading ?? undefined,
              speed: pos.coords.speed !== null ? pos.coords.speed * 3.6 : undefined, // m/s→km/h
              accuracy: pos.coords.accuracy,
              device: 'BROWSER',
              recordedAt: pos.timestamp,
            }
            client.publish({
              destination: `/app/journey/${journeyId}/location`,
              body: JSON.stringify(payload),
            })
            setLastSent(Date.now())
          },
          (geoErr) => setError(`Location error: ${geoErr.message}`),
          { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
        )
      },
      onStompError: (frame) =>
        setError(frame.headers?.message ?? 'WebSocket error'),
    })

    clientRef.current = client
    client.activate()
  }, [journeyId])

  // Cleanup on unmount
  useEffect(() => () => stop(), [stop])

  return { start, stop, sending, lastSent, error }
}
