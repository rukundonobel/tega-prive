import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2 } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Input, Spinner } from '@/components/ui'
import type { Complaint } from '@/types'

export default function AdminComplaints() {
  const qc = useQueryClient()
  const { data: complaints, isLoading } = useQuery({
    queryKey: ['admin', 'complaints'],
    queryFn: adminService.complaints,
  })

  const [resolvingId, setResolvingId] = useState<string | null>(null)
  const [resolution, setResolution] = useState('')

  const resolve = useMutation({
    mutationFn: ({ id, text }: { id: string; text: string }) =>
      adminService.resolveComplaint(id, text),
    onSuccess: () => {
      setResolvingId(null)
      setResolution('')
      qc.invalidateQueries({ queryKey: ['admin', 'complaints'] })
    },
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Complaints</h1>
        <p className="mt-1 text-sm text-ink/60">Review and resolve passenger and driver complaints.</p>
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="space-y-3">
        {(complaints ?? []).map((c) => (
          <ComplaintCard
            key={c.id}
            complaint={c}
            isResolving={resolvingId === c.id}
            resolution={resolution}
            onResolutionChange={setResolution}
            onStartResolve={() => setResolvingId(c.id)}
            onCancel={() => setResolvingId(null)}
            onConfirm={() => resolve.mutate({ id: c.id, text: resolution })}
            loading={resolve.isPending}
          />
        ))}
        {complaints && complaints.length === 0 && (
          <div className="card p-12 text-center text-sm text-ink/50">No complaints on record.</div>
        )}
      </div>
    </div>
  )
}

function ComplaintCard({
  complaint: c, isResolving, resolution, onResolutionChange,
  onStartResolve, onCancel, onConfirm, loading
}: {
  complaint: Complaint; isResolving: boolean; resolution: string;
  onResolutionChange: (v: string) => void;
  onStartResolve: () => void; onCancel: () => void;
  onConfirm: () => void; loading: boolean
}) {
  return (
    <div className="card p-5 space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <span className="font-semibold text-forest">{c.complainantName}</span>
            <span className="text-sm text-ink/40">({c.complainantEmail})</span>
          </div>
          <span className="mt-1 inline-block rounded-full bg-mist px-2.5 py-1 text-xs text-ink/60">
            {c.category} · {c.subjectType}
          </span>
        </div>
        <StatusBadge status={c.status} />
      </div>
      <p className="text-sm text-ink/70">{c.description}</p>
      {c.resolution && (
        <div className="rounded-xl bg-forest/5 px-4 py-3 text-sm text-forest">
          <strong>Resolution:</strong> {c.resolution}
        </div>
      )}
      {c.status === 'OPEN' && !isResolving && (
        <Button size="sm" onClick={onStartResolve}>
          <CheckCircle2 className="h-4 w-4" /> Resolve
        </Button>
      )}
      {isResolving && (
        <div className="space-y-3">
          <Input
            label="Resolution"
            value={resolution}
            onChange={(e) => onResolutionChange(e.target.value)}
            placeholder="Describe how this complaint was resolved…"
          />
          <div className="flex gap-2">
            <Button size="sm" loading={loading} onClick={onConfirm}>Confirm</Button>
            <Button size="sm" variant="ghost" onClick={onCancel}>Cancel</Button>
          </div>
        </div>
      )}
    </div>
  )
}
