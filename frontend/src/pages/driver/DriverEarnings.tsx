import { useQuery } from '@tanstack/react-query'
import { TrendingUp, Clock } from 'lucide-react'
import { commissionsService } from '@/services/payments.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Spinner } from '@/components/ui'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', {
    style: 'currency', currency: 'RWF', maximumFractionDigits: 0,
  }).format(n)
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { year: 'numeric', month: 'short', day: 'numeric' })
}

export default function DriverEarnings() {
  const { data: earnings, isLoading } = useQuery({
    queryKey: ['driver-earnings'],
    queryFn: commissionsService.myEarnings,
  })

  const { data: balance } = useQuery({
    queryKey: ['earnings-balance'],
    queryFn: commissionsService.pendingBalance,
  })

  const { data: rule } = useQuery({
    queryKey: ['commission-rule'],
    queryFn: commissionsService.currentRule,
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">My earnings</h1>
        <p className="mt-1 text-sm text-ink/60">Track your income and pending settlements.</p>
      </div>

      {/* Summary cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="card p-5">
          <p className="text-xs uppercase tracking-wide text-ink/40">Pending settlement</p>
          <p className="mt-2 font-display text-2xl font-semibold text-forest">
            {balance !== undefined ? formatRwf(balance) : '—'}
          </p>
          <p className="mt-1 flex items-center gap-1 text-xs text-ink/50">
            <Clock className="h-3.5 w-3.5" /> Awaiting payout
          </p>
        </div>
        <div className="card p-5">
          <p className="text-xs uppercase tracking-wide text-ink/40">Total trips earned</p>
          <p className="mt-2 font-display text-2xl font-semibold text-forest">
            {earnings?.length ?? 0}
          </p>
        </div>
        {rule && (
          <div className="card p-5">
            <p className="text-xs uppercase tracking-wide text-ink/40">Platform commission</p>
            <p className="mt-2 font-display text-2xl font-semibold text-forest">
              {(rule.driverCommissionPct * 100).toFixed(0)}%
            </p>
            <p className="mt-1 text-xs text-ink/50">per completed trip</p>
          </div>
        )}
      </div>

      {/* Earnings table */}
      <div className="card overflow-hidden">
        <div className="border-b border-line px-5 py-4">
          <h2 className="font-semibold text-forest">Earnings history</h2>
        </div>

        {isLoading && (
          <div className="grid place-items-center py-12"><Spinner /></div>
        )}

        {earnings && earnings.length === 0 && (
          <div className="p-10 text-center text-sm text-ink/50">
            No earnings yet. Complete a journey to start earning.
          </div>
        )}

        <ul className="divide-y divide-line">
          {earnings?.map((e) => (
            <li key={e.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
              <div>
                <p className="font-medium text-forest">Booking #{e.bookingId.slice(0, 8).toUpperCase()}</p>
                <p className="text-xs text-ink/50">{formatDate(e.createdAt)}</p>
              </div>
              <div className="text-right">
                <p className="font-semibold text-forest">{formatRwf(e.netEarnings)}</p>
                <div className="flex items-center justify-end gap-1 text-xs text-ink/40">
                  <TrendingUp className="h-3 w-3 text-red-400" />
                  Commission: {formatRwf(e.commission)}
                </div>
              </div>
              <StatusBadge status={e.settlementStatus} />
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
