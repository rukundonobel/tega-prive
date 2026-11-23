import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Car, Edit, FileUp, Send, Trash2 } from 'lucide-react'
import { vehiclesService } from '@/services/vehicles.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Spinner } from '@/components/ui'
import type { ApiError, VehicleDocumentType } from '@/types'

const DOC_TYPES: VehicleDocumentType[] = ['REGISTRATION', 'INSURANCE', 'OTHER']

export default function VehicleDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const { data: vehicle, isLoading } = useQuery({
    queryKey: ['vehicle', id],
    queryFn: () => vehiclesService.get(id!),
    enabled: !!id,
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['vehicle', id] })
    qc.invalidateQueries({ queryKey: ['vehicles'] })
  }

  const photoMut = useMutation({
    mutationFn: (file: File) => vehiclesService.addPhoto(id!, file),
    onSuccess: invalidate,
  })
  const removePhotoMut = useMutation({
    mutationFn: (photoId: string) => vehiclesService.removePhoto(id!, photoId),
    onSuccess: invalidate,
  })
  const docMut = useMutation({
    mutationFn: ({ type, file }: { type: string; file: File }) =>
      vehiclesService.addDocument(id!, type, file),
    onSuccess: invalidate,
  })
  const submitMut = useMutation({
    mutationFn: () => vehiclesService.submit(id!),
    onSuccess: invalidate,
    onError: (e) => alert((e as ApiError).message),
  })
  const deleteMut = useMutation({
    mutationFn: () => vehiclesService.remove(id!),
    onSuccess: () => navigate('/driver/vehicles'),
    onError: (e) => alert((e as ApiError).message),
  })

  if (isLoading || !vehicle) {
    return (
      <div className="grid place-items-center py-16">
        <Spinner />
      </div>
    )
  }

  const editable = vehicle.approvalStatus !== 'APPROVED'
  const canSubmit = vehicle.approvalStatus === 'DRAFT' || vehicle.approvalStatus === 'REJECTED'

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to="/driver/vehicles" className="text-sm text-ink/50 hover:text-forest">
            ← My vehicles
          </Link>
          <h1 className="mt-1 font-display text-2xl font-semibold text-forest">
            {vehicle.make} {vehicle.model}
          </h1>
          <div className="mt-2 flex items-center gap-3">
            <StatusBadge status={vehicle.approvalStatus} />
            <span className="text-sm text-ink/50">{vehicle.registrationNumber}</span>
          </div>
        </div>
        {editable && (
          <Link to={`/driver/vehicles/${vehicle.id}/edit`}>
            <Button variant="outline">
              <Edit className="h-4 w-4" /> Edit details
            </Button>
          </Link>
        )}
      </div>

      {vehicle.rejectionReason && (
        <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          <span className="font-medium">Rejected:</span> {vehicle.rejectionReason}
        </div>
      )}

      {/* Specs */}
      <div className="card grid grid-cols-2 gap-4 p-5 sm:grid-cols-4">
        <Spec label="Category" value={vehicle.category} />
        <Spec label="Seats" value={`${vehicle.seatCapacity}`} />
        <Spec label="Luggage" value={`${vehicle.luggageCapacity}`} />
        <Spec label="A/C" value={vehicle.hasAc ? 'Yes' : 'No'} />
      </div>
      {vehicle.description && <p className="text-sm text-ink/60">{vehicle.description}</p>}

      {/* Photos */}
      <div className="card space-y-4 p-6">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-forest">Photos</h2>
          {editable && (
            <label className="cursor-pointer">
              <span className="btn border border-forest/20 px-4 py-2 text-sm text-forest hover:bg-forest hover:text-white">
                <FileUp className="h-4 w-4" /> Upload photo
              </span>
              <input
                type="file"
                accept="image/*"
                className="hidden"
                disabled={photoMut.isPending}
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) photoMut.mutate(f)
                }}
              />
            </label>
          )}
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {vehicle.photos.map((p) => (
            <div key={p.id} className="relative overflow-hidden rounded-xl border border-line">
              <img src={p.url} alt="Vehicle" className="aspect-[4/3] w-full object-cover" />
              {p.isPrimary && <span className="absolute left-2 top-2 rounded-full bg-gold px-2 py-0.5 text-xs text-forest">Primary</span>}
              {editable && (
                <button
                  onClick={() => removePhotoMut.mutate(p.id)}
                  className="absolute right-2 top-2 grid h-7 w-7 place-items-center rounded-full bg-white/90 text-red-600 hover:bg-white"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
            </div>
          ))}
          {vehicle.photos.length === 0 && (
            <div className="col-span-full grid place-items-center rounded-xl border border-dashed border-line py-10 text-ink/40">
              <Car className="h-8 w-8" />
              <p className="mt-2 text-sm">No photos yet.</p>
            </div>
          )}
        </div>
      </div>

      {/* Documents */}
      <div className="card space-y-4 p-6">
        <h2 className="font-semibold text-forest">Documents</h2>
        <ul className="divide-y divide-line">
          {vehicle.documents.map((d) => (
            <li key={d.id} className="flex items-center justify-between py-3">
              <div>
                <p className="font-medium text-forest">{d.type}</p>
                <a href={d.url} target="_blank" rel="noreferrer" className="text-sm text-gold-dark hover:underline">
                  View document
                </a>
              </div>
              <StatusBadge status={d.status} />
            </li>
          ))}
          {vehicle.documents.length === 0 && (
            <li className="py-4 text-center text-sm text-ink/50">No documents uploaded yet.</li>
          )}
        </ul>
        {editable && (
          <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
            <div>
              <label className="label">Document type</label>
              <select id="vehDocType" className="input-base" defaultValue="REGISTRATION">
                {DOC_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t.charAt(0) + t.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            </div>
            <label className="cursor-pointer">
              <span className="btn border border-forest/20 px-4 py-2 text-sm text-forest hover:bg-forest hover:text-white">
                <FileUp className="h-4 w-4" /> Choose file
              </span>
              <input
                type="file"
                accept="image/*,application/pdf"
                className="hidden"
                disabled={docMut.isPending}
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  const sel = document.getElementById('vehDocType') as HTMLSelectElement | null
                  if (f && sel) docMut.mutate({ type: sel.value, file: f })
                }}
              />
            </label>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex flex-wrap gap-3">
        {canSubmit && (
          <Button onClick={() => submitMut.mutate()} loading={submitMut.isPending}>
            <Send className="h-4 w-4" /> Submit for approval
          </Button>
        )}
        {editable && (
          <Button
            variant="danger"
            onClick={() => {
              if (confirm('Delete this vehicle? This cannot be undone.')) deleteMut.mutate()
            }}
            loading={deleteMut.isPending}
          >
            <Trash2 className="h-4 w-4" /> Delete vehicle
          </Button>
        )}
      </div>
    </div>
  )
}

function Spec({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-ink/40">{label}</p>
      <p className="mt-1 font-medium text-forest">{value}</p>
    </div>
  )
}
