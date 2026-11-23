import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2 } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { Button, Spinner } from '@/components/ui'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}

export default function AdminPayments() {
  const qc = useQueryClient()
  const { data: payouts, isLoading } = useQuery({
    queryKey: ['admin', 'pendingPayouts'],
    queryFn: adminService.pendingPayouts,
  })

  const markPaid = useMutation({
    mutationFn: (id: string) => adminService.markPayoutPaid(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'pendingPayouts'] }),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Payments & payouts</h1>
        <p className="mt-1 text-sm text-ink/60">Manage pending driver payouts.</p>
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="space-y-3">
        {(payouts ?? []).map((p) => (
          <div key={p.id} className="card flex flex-wrap items-center justify-between gap-3 p-5">
            <div>
              <p className="font-semibold text-forest">{p.driverName}</p>
              <p className="text-xs text-ink/50">{p.provider ?? 'Manual'} · {p.phoneNumber ?? '—'}</p>
            </div>
            <p className="font-display text-xl font-semibold text-forest">{formatRwf(p.amount)}</p>
            <Button size="sm" loading={markPaid.isPending} onClick={() => markPaid.mutate(p.id)}>
              <CheckCircle2 className="h-4 w-4" /> Mark paid
            </Button>
          </div>
        ))}
        {payouts && payouts.length === 0 && (
          <div className="card p-12 text-center text-sm text-ink/50">No pending payouts.</div>
        )}
      </div>
    </div>
  )
}
