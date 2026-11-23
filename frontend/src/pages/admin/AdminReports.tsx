import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { TrendingUp, DollarSign, BookOpen, Users } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { Spinner } from '@/components/ui'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF', maximumFractionDigits: 0 }).format(n)
}

export default function AdminReports() {
  const [days, setDays] = useState(30)

  const { data: report, isLoading } = useQuery({
    queryKey: ['admin', 'revenue', days],
    queryFn: () => adminService.revenueReport(days),
  })

  const { data: rules } = useQuery({
    queryKey: ['admin', 'commissionRules'],
    queryFn: adminService.commissionRules,
  })

  const currentRule = rules?.[0]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Revenue reports</h1>
        <p className="mt-1 text-sm text-ink/60">Platform financial performance.</p>
      </div>

      {/* Period selector */}
      <div className="flex flex-wrap gap-2">
        {[7, 14, 30, 90].map((d) => (
          <button
            key={d}
            onClick={() => setDays(d)}
            className={`rounded-xl border px-4 py-2 text-sm font-medium transition ${
              days === d
                ? 'border-forest bg-forest text-white'
                : 'border-line text-ink/70 hover:border-forest/30'
            }`}
          >
            {d}d
          </button>
        ))}
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      {report && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <ReportCard icon={TrendingUp} label="Gross fare collected" value={formatRwf(report.grossFare)} />
          <ReportCard icon={DollarSign} label="Platform revenue" value={formatRwf(report.platformRevenue)} accent />
          <ReportCard icon={Users} label="Driver earnings" value={formatRwf(report.driverEarnings)} />
          <ReportCard icon={BookOpen} label="Paid bookings" value={report.bookingCount.toString()} />
        </div>
      )}

      {/* Commission rules */}
      <div className="card overflow-hidden">
        <div className="border-b border-line px-5 py-4">
          <h2 className="font-semibold text-forest">Active commission rule</h2>
        </div>
        {currentRule ? (
          <div className="grid gap-6 p-5 sm:grid-cols-3">
            <Metric label="Driver commission" value={`${(currentRule.driverCommissionPct * 100).toFixed(1)}%`} />
            <Metric label="Passenger service fee" value={`${(currentRule.passengerServiceFeePct * 100).toFixed(1)}%`} />
            <Metric label="Fixed booking fee" value={formatRwf(currentRule.fixedBookingFee)} />
          </div>
        ) : (
          <p className="p-5 text-sm text-ink/50">No commission rule configured.</p>
        )}
      </div>
    </div>
  )
}

function ReportCard({ icon: Icon, label, value, accent = false }: {
  icon: React.ElementType; label: string; value: string; accent?: boolean
}) {
  return (
    <div className={`card p-5 ${accent ? 'border-gold/40 bg-gold/5' : ''}`}>
      <span className="grid h-10 w-10 place-items-center rounded-xl bg-gold/15 text-gold-dark">
        <Icon className="h-5 w-5" />
      </span>
      <p className="mt-3 text-xs uppercase tracking-wide text-ink/40">{label}</p>
      <p className="mt-1 font-display text-2xl font-semibold text-forest">{value}</p>
    </div>
  )
}
function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-ink/40">{label}</p>
      <p className="mt-1 font-display text-xl font-semibold text-forest">{value}</p>
    </div>
  )
}
