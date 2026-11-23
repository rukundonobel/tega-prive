import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { vehiclesService, type VehiclePayload } from '@/services/vehicles.service'
import type { ApiError, Vehicle, VehicleCategory } from '@/types'
import { Button, Input, Spinner } from '@/components/ui'

const CATEGORIES: VehicleCategory[] = ['SEDAN', 'SUV', 'VAN', 'MINIBUS', 'LUXURY', 'OTHER']

function toPayload(v: Vehicle): VehiclePayload {
  return {
    make: v.make,
    model: v.model,
    manufacturingYear: v.manufacturingYear,
    colour: v.colour,
    registrationNumber: v.registrationNumber,
    category: v.category,
    seatCapacity: v.seatCapacity,
    luggageCapacity: v.luggageCapacity,
    hasAc: v.hasAc,
    description: v.description,
  }
}

const empty: VehiclePayload = {
  make: '',
  model: '',
  manufacturingYear: undefined,
  colour: '',
  registrationNumber: '',
  category: 'SEDAN',
  seatCapacity: 4,
  luggageCapacity: 2,
  hasAc: false,
  description: '',
}

export default function VehicleForm() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isEdit = !!id

  const { data: vehicle } = useQuery({
    queryKey: ['vehicle', id],
    queryFn: () => vehiclesService.get(id!),
    enabled: isEdit,
  })

  const [form, setForm] = useState<VehiclePayload>(empty)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (vehicle) setForm(toPayload(vehicle))
  }, [vehicle])

  const saveMut = useMutation({
    mutationFn: (p: VehiclePayload) =>
      isEdit ? vehiclesService.update(id!, p) : vehiclesService.create(p),
    onSuccess: (v) => navigate(`/driver/vehicles/${v.id}`),
    onError: (e) => setError((e as ApiError).message),
  })

  const set = <K extends keyof VehiclePayload>(k: K, v: VehiclePayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  const submit = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    saveMut.mutate(form)
  }

  if (isEdit && !vehicle) {
    return (
      <div className="grid place-items-center py-16">
        <Spinner />
      </div>
    )
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">
          {isEdit ? 'Edit vehicle' : 'Add a vehicle'}
        </h1>
        <p className="mt-1 text-sm text-ink/60">
          Register your vehicle, then upload photos and a registration document before submitting.
        </p>
      </div>

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <form onSubmit={submit} className="card space-y-4 p-6">
        <div className="grid grid-cols-2 gap-4">
          <Input label="Make" value={form.make} onChange={(e) => set('make', e.target.value)} required />
          <Input label="Model" value={form.model} onChange={(e) => set('model', e.target.value)} required />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Manufacturing year"
            type="number"
            value={form.manufacturingYear ?? ''}
            onChange={(e) => set('manufacturingYear', e.target.value ? Number(e.target.value) : undefined)}
          />
          <Input label="Colour" value={form.colour} onChange={(e) => set('colour', e.target.value)} required />
        </div>
        <Input
          label="Registration number"
          value={form.registrationNumber}
          onChange={(e) => set('registrationNumber', e.target.value.toUpperCase())}
          required
        />
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="label">Category</label>
            <select
              className="input-base"
              value={form.category}
              onChange={(e) => set('category', e.target.value as VehicleCategory)}
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c.charAt(0) + c.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Seats"
              type="number"
              value={form.seatCapacity}
              onChange={(e) => set('seatCapacity', Number(e.target.value))}
            />
            <Input
              label="Luggage"
              type="number"
              value={form.luggageCapacity}
              onChange={(e) => set('luggageCapacity', Number(e.target.value))}
            />
          </div>
        </div>
        <label className="flex items-center gap-2 text-sm text-ink/80">
          <input
            type="checkbox"
            checked={form.hasAc}
            onChange={(e) => set('hasAc', e.target.checked)}
            className="h-4 w-4 rounded border-line accent-gold"
          />
          Air conditioning available
        </label>
        <div>
          <label className="label">Description</label>
          <textarea
            className="input-base min-h-24"
            value={form.description}
            onChange={(e) => set('description', e.target.value)}
          />
        </div>
        <div className="flex gap-3">
          <Button type="submit" loading={saveMut.isPending}>
            {isEdit ? 'Save changes' : 'Create vehicle'}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate('/driver/vehicles')}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  )
}
