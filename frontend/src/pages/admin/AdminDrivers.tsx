import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, X } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Input, Spinner } from '@/components/ui'
import type { ApiError } from '@/types'

export default function AdminDrivers() {
  const qc = useQueryClient()
  const { data: drivers, isLoading } = useQuery({
    queryKey: ['admin', 'pendingDrivers'],
    queryFn: adminService.pendingDrivers,
  })
  const [rejectingId, setRejectingId] = useState<string | null>(null)
  const [reason, setReason] = useState('')

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin', 'pendingDrivers'] })

  const approveMut = useMutation({
    mutationFn: (id: string) => adminService.approveDriver(id),
    onSuccess: invalidate,
  })
  const rejectMut = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => adminService.rejectDriver(id, reason),
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
        <h1 className="font-display text-2xl font-semibold text-forest">Driver verification</h1>
        <p className="mt-1 text-sm text-ink/60">Review and approve drivers waiting for verification.</p>
      </div>

      {isLoading && (
        <div className="grid place-items-center py-16">
          <Spinner />
        </div>
      )}

      <div className="space-y-3">
        {(drivers ?? []).map((d) => (
          <div key={d.id} className="card p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-semibold text-forest">{d.name}</p>
                <p className="text-sm text-ink/60">
                  {d.email} · Licence {d.licenceNumber ?? '—'}
                </p>
              </div>
              <StatusBadge status={d.verificationStatus} />
            </div>

            {rejectingId === d.id ? (
              <div className="mt-4 flex flex-wrap items-end gap-3">
                <div className="flex-1 min-w-48">
                  <Input
                    label="Reason for rejection"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="e.g. Licence image is blurry"
                  />
                </div>
                <Button
                  variant="danger"
                  loading={rejectMut.isPending}
                  onClick={() => rejectMut.mutate({ id: d.id, reason })}
                >
                  Confirm reject
                </Button>
                <Button variant="ghost" onClick={() => setRejectingId(null)}>
                  Cancel
                </Button>
              </div>
            ) : (
              <div className="mt-4 flex gap-2">
                <Button size="sm" onClick={() => approveMut.mutate(d.id)} loading={approveMut.isPending}>
                  <Check className="h-4 w-4" /> Approve
                </Button>
                <Button size="sm" variant="outline" onClick={() => setRejectingId(d.id)}>
                  <X className="h-4 w-4" /> Reject
                </Button>
              </div>
            )}
          </div>
        ))}

        {drivers && drivers.length === 0 && (
          <div className="card p-12 text-center text-sm text-ink/50">No drivers pending verification.</div>
        )}
      </div>
    </div>
  )
}
