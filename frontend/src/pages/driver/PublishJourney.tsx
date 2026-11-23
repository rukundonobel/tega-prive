import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { journeysService, type PublishJourneyPayload } from '@/services/journeys.service'
import { locationsService } from '@/services/journeys.service'
import { vehiclesService } from '@/services/vehicles.service'
import { Button, Input } from '@/components/ui'
import type { ApiError } from '@/types'

export default function PublishJourney() {
  const navigate = useNavigate()
  const [form, setForm] = useState<Partial<PublishJourneyPayload>>({
    totalSeats: 4,
    pricePerSeat: 0,
  })
  const [error, setError] = useState<string | null>(null)

  const { data: locations } = useQuery({ queryKey: ['locations'], queryFn: locationsService.list })
  const { data: vehicles } = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesService.list })
  const approvedVehicles = (vehicles ?? []).filter((v) => v.approvalStatus === 'APPROVED')

  const set = <K extends keyof PublishJourneyPayload>(k: K, v: PublishJourneyPayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  const publishMut = useMutation({
    mutationFn: (p: PublishJourneyPayload) => journeysService.publish(p),
    onSuccess: () => navigate('/driver/journeys'),
    onError: (e) => setError((e as ApiError).message),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!form.vehicleId || !form.originId || !form.destinationId || !form.departureTime) {
      setError('Please fill in all required fields.')
      return
    }
    publishMut.mutate(form as PublishJourneyPayload)
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Publish a journey</h1>
        <p className="mt-1 text-sm text-ink/60">
          Only approved vehicles can be used. Your driver profile must be verified.
        </p>
      </div>

      {approvedVehicles.length === 0 && (
        <div className="rounded-xl bg-gold/10 px-4 py-4 text-sm text-gold-dark">
          You have no approved vehicles. Add a vehicle, upload photos + registration document, and
          submit for approval before publishing journeys.
        </div>
      )}

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <form onSubmit={submit} className="card space-y-4 p-5 sm:p-6">
        {/* Vehicle */}
        <div>
          <label className="label">Vehicle *</label>
          <select
            className="input-base"
            value={form.vehicleId ?? ''}
            onChange={(e) => set('vehicleId', e.target.value)}
            required
          >
            <option value="">Select an approved vehicle</option>
            {approvedVehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.make} {v.model} — {v.registrationNumber}
              </option>
            ))}
          </select>
        </div>

        {/* Origin / destination */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="label">From *</label>
            <select
              className="input-base"
              value={form.originId ?? ''}
              onChange={(e) => set('originId', e.target.value)}
              required
            >
              <option value="">Select departure</option>
              {locations?.map((l) => (
                <option key={l.id} value={l.id}>{l.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">To *</label>
            <select
              className="input-base"
              value={form.destinationId ?? ''}
              onChange={(e) => set('destinationId', e.target.value)}
              required
            >
              <option value="">Select destination</option>
              {locations?.map((l) => (
                <option key={l.id} value={l.id}>{l.name}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Departure / arrival */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Departure date & time *</label>
            <input
              type="datetime-local"
              className="input-base"
              value={form.departureTime?.slice(0, 16) ?? ''}
              onChange={(e) =>
                set('departureTime', new Date(e.target.value).toISOString())
              }
              required
            />
          </div>
          <div>
            <label className="label">Estimated arrival (optional)</label>
            <input
              type="datetime-local"
              className="input-base"
              value={form.estimatedArrival?.slice(0, 16) ?? ''}
              onChange={(e) =>
                set('estimatedArrival', e.target.value ? new Date(e.target.value).toISOString() : undefined)
              }
            />
          </div>
        </div>

        {/* Price / seats */}
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Price per seat (RWF) *"
            type="number"
            min={0}
            step={100}
            value={form.pricePerSeat ?? ''}
            onChange={(e) => set('pricePerSeat', Number(e.target.value))}
            required
          />
          <Input
            label="Total seats *"
            type="number"
            min={1}
            max={50}
            value={form.totalSeats ?? ''}
            onChange={(e) => set('totalSeats', Number(e.target.value))}
            required
          />
        </div>

        {/* Instructions */}
        <div>
          <label className="label">Pickup instructions</label>
          <textarea
            className="input-base min-h-20"
            placeholder="e.g. Meet at Nyabugogo bus terminal, gate 3"
            value={form.pickupInstructions ?? ''}
            onChange={(e) => set('pickupInstructions', e.target.value)}
          />
        </div>
        <div>
          <label className="label">Dropoff instructions</label>
          <textarea
            className="input-base min-h-20"
            placeholder="e.g. I drop off at Rubavu market"
            value={form.dropoffInstructions ?? ''}
            onChange={(e) => set('dropoffInstructions', e.target.value)}
          />
        </div>

        <div className="flex gap-3">
          <Button type="submit" loading={publishMut.isPending} disabled={approvedVehicles.length === 0}>
            Publish journey
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/driver/journeys')}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  )
}
