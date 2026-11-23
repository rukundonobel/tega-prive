import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Users, Car, ShieldCheck, BookOpen, TrendingUp,
  Navigation, MessageSquare, DollarSign, ArrowRight, Loader2
} from 'lucide-react'
import { adminService } from '@/services/admin.service'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', {
    style: 'currency', currency: 'RWF', maximumFractionDigits: 0,
  }).format(n)
}
function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function StatCard({
  label, value, sub, to, icon: Icon, accent = false
}: {
  label: string; value: string | number; sub?: string; to?: string;
  icon: React.ElementType; accent?: boolean
}) {
  const inner = (
    <div className={`card p-5 ${accent ? 'border-gold/40' : ''} ${to ? 'transition hover:shadow-card-hover' : ''}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-ink/40">{label}</p>
          <p className="mt-1 font-display text-2xl font-semibold text-forest">{value}</p>
          {sub && <p className="mt-0.5 text-xs text-ink/50">{sub}</p>}
        </div>
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-gold/15 text-gold-dark">
          <Icon className="h-5 w-5" />
        </span>
      </div>
      {to && (
        <p className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-gold-dark">
          View <ArrowRight className="h-3.5 w-3.5" />
        </p>
      )}
    </div>
  )
  return to ? <Link to={to}>{inner}</Link> : inner
}

export default function AdminOverview() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: adminService.stats,
    refetchInterval: 30_000,
  })

  const { data: bookings } = useQuery({
    queryKey: ['admin', 'recentBookings'],
    queryFn: () => adminService.recentBookings(10),
  })

  if (isLoading) {
    return (
      <div className="grid place-items-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-gold" />
      </div>
    )
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Admin dashboard</h1>
        <p className="mt-1 text-sm text-ink/60">Platform overview and quick actions.</p>
      </div>

      {/* KPI grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total users" value={stats?.totalUsers ?? 0} icon={Users} to="/admin/users" />
        <StatCard
          label="Drivers"
          value={stats?.totalDrivers ?? 0}
          sub={`${stats?.verifiedDrivers ?? 0} verified`}
          icon={ShieldCheck}
          to="/admin/drivers"
        />
        <StatCard
          label="Vehicles"
          value={stats?.totalVehicles ?? 0}
          sub={`${stats?.approvedVehicles ?? 0} approved`}
          icon={Car}
          to="/admin/vehicles"
        />
        <StatCard
          label="Bookings"
          value={stats?.totalBookings ?? 0}
          sub={`${stats?.confirmedBookings ?? 0} confirmed`}
          icon={BookOpen}
          to="/admin/bookings"
        />
        <StatCard
          label="Platform revenue"
          value={formatRwf(stats?.totalPlatformRevenue ?? 0)}
          icon={TrendingUp}
          accent
          to="/admin/reports"
        />
        <StatCard
          label="Driver earnings"
          value={formatRwf(stats?.totalDriverEarnings ?? 0)}
          icon={DollarSign}
          to="/admin/payments"
        />
        <StatCard
          label="Active journeys"
          value={stats?.activeJourneys ?? 0}
          icon={Navigation}
          to="/admin/monitoring"
        />
        <StatCard
          label="Open complaints"
          value={stats?.pendingComplaints ?? 0}
          icon={MessageSquare}
          accent={!!stats?.pendingComplaints}
          to="/admin/complaints"
        />
      </div>

      {/* Recent bookings */}
      <div className="card overflow-hidden">
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <h2 className="font-semibold text-forest">Recent bookings</h2>
          <Link to="/admin/bookings" className="text-sm text-gold-dark hover:text-gold">
            View all →
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-mist text-left text-xs uppercase tracking-wide text-ink/40">
              <tr>
                <th className="px-5 py-3">Route</th>
                <th className="px-5 py-3">Passenger</th>
                <th className="px-5 py-3">Driver</th>
                <th className="px-5 py-3 text-right">Amount</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {(bookings ?? []).map((b) => (
                <tr key={b.id} className="hover:bg-mist/50">
                  <td className="px-5 py-3 font-medium text-forest">
                    {b.origin} → {b.destination}
                  </td>
                  <td className="px-5 py-3 text-ink/70">{b.passengerName}</td>
                  <td className="px-5 py-3 text-ink/70">{b.driverName}</td>
                  <td className="px-5 py-3 text-right font-medium">{formatRwf(b.totalAmount)}</td>
                  <td className="px-5 py-3">
                    <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                      b.status === 'CONFIRMED' || b.status === 'COMPLETED'
                        ? 'bg-forest/10 text-forest'
                        : b.status === 'CANCELLED' ? 'bg-red-100 text-red-700'
                        : 'bg-gold/15 text-gold-dark'
                    }`}>
                      {b.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-ink/50">{formatDate(b.createdAt)}</td>
                </tr>
              ))}
              {!bookings?.length && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-ink/40">No bookings yet.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
