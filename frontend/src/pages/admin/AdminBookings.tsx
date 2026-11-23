import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Spinner } from '@/components/ui'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
}

export default function AdminBookings() {
  const [q, setQ] = useState('')
  const { data: bookings, isLoading } = useQuery({ queryKey: ['admin', 'allBookings'], queryFn: adminService.allBookings })

  const filtered = (bookings ?? []).filter((b) =>
    !q || b.passengerName?.toLowerCase().includes(q.toLowerCase()) ||
    b.origin.toLowerCase().includes(q.toLowerCase()) ||
    b.destination.toLowerCase().includes(q.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Booking management</h1>
        <p className="mt-1 text-sm text-ink/60">All platform bookings across all journeys.</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/30" />
        <input
          className="input-base pl-9"
          placeholder="Search route, passenger…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-mist text-left text-xs uppercase tracking-wide text-ink/40">
              <tr>
                <th className="px-5 py-3">Route</th>
                <th className="px-5 py-3">Passenger</th>
                <th className="px-5 py-3">Seats</th>
                <th className="px-5 py-3 text-right">Total</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {filtered.map((b) => (
                <tr key={b.id} className="hover:bg-mist/50">
                  <td className="px-5 py-3 font-medium text-forest">{b.origin} → {b.destination}</td>
                  <td className="px-5 py-3 text-ink/60">{b.passengerName}</td>
                  <td className="px-5 py-3 text-ink/60">{b.seatsBooked}</td>
                  <td className="px-5 py-3 text-right font-medium">{formatRwf(b.totalAmount)}</td>
                  <td className="px-5 py-3"><StatusBadge status={b.status} /></td>
                  <td className="px-5 py-3 text-ink/50">{formatDate(b.createdAt)}</td>
                </tr>
              ))}
              {!filtered.length && (
                <tr><td colSpan={6} className="px-5 py-10 text-center text-ink/40">No bookings found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
