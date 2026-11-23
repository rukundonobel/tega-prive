import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, X, Car } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Input, Spinner } from '@/components/ui'
import type { ApiError } from '@/types'

export default function AdminVehicles() {
  const qc = useQueryClient()
  const { data: vehicles, isLoading } = useQuery({
    queryKey: ['admin', 'pendingVehicles'],
    queryFn: adminService.pendingVehicles,
  })
  const [rejectingId, setRejectingId] = useState<string | null>(null)
  const [reason, setReason] = useState('')

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin', 'pendingVehicles'] })

  const approveMut = useMutation({
    mutationFn: (id: string) => adminService.approveVehicle(id),
    onSuccess: invalidate,
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => adminService.rejectVehicle(id, reason),
    onSuccess: () => {
      setRejectingId(null)
      setReason('')
      invalidate()
    },
    onError: (e) => alert((e as ApiError).message),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Vehicle approval</h1>
        <p className="mt-1 text-sm text-ink/60">Review vehicles submitted for approval.</p>
      </div>

      {isLoading && (
        <div className="grid place-items-center py-16">
          <Spinner />
        </div>
      )}

      <div className="space-y-3">
        {(vehicles ?? []).map((v) => (
          <div key={v.id} className="card p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="grid h-16 w-16 shrink-0 place-items-center overflow-hidden rounded-xl bg-mist">
                  {v.photos[0] ? (
                    <img src={v.photos[0].url} alt={`${v.make} ${v.model}`} className="h-full w-full object-cover" />
                  ) : (
                    <Car className="h-6 w-6 text-ink/30" />
                  )}
                </div>
                <div>
                  <p className="font-semibold text-forest">
                    {v.make} {v.model} <span className="text-ink/40">· {v.registrationNumber}</span>
                  </p>
                  <p className="text-sm text-ink/60">
                    {v.driverName} · {v.category} · {v.seatCapacity} seats
                  </p>
                </div>
              </div>
              <StatusBadge status={v.approvalStatus} />
            </div>

            {v.documents.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-3">
                {v.documents.map((d) => (
                  <a
                    key={d.id}
                    href={d.url}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-lg border border-line px-3 py-1.5 text-xs font-medium text-forest hover:bg-mist"
                  >
                    {d.type} ↗
                  </a>
                ))}
              </div>
            )}

            {rejectingId === v.id ? (
              <div className="mt-4 flex flex-wrap items-end gap-3">
                <div className="min-w-48 flex-1">
                  <Input
                    label="Reason for rejection"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="e.g. Insurance document missing"
                  />
                </div>
                <Button
                  variant="danger"
                  loading={rejectMut.isPending}
                  onClick={() => rejectMut.mutate({ id: v.id, reason })}
                >
                  Confirm reject
                </Button>
                <Button variant="ghost" onClick={() => setRejectingId(v.id)}>
                  Cancel
                </Button>
              </div>
            ) : (
              <div className="mt-4 flex gap-2">
                <Button size="sm" onClick={() => approveMut.mutate(v.id)} loading={approveMut.isPending}>
                  <Check className="h-4 w-4" /> Approve
                </Button>
                <Button size="sm" variant="outline" onClick={() => setRejectingId(v.id)}>
                  <X className="h-4 w-4" /> Reject
                </Button>
              </div>
            )}
          </div>
        ))}

        {vehicles && vehicles.length === 0 && (
          <div className="card p-12 text-center text-sm text-ink/50">No vehicles pending approval.</div>
        )}
      </div>
    </div>
  )
}
