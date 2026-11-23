import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Car, Plus, ShieldCheck, ArrowRight } from 'lucide-react'
import { driversService } from '@/services/drivers.service'
import { vehiclesService } from '@/services/vehicles.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui'

export default function DriverDashboard() {
  const { data: driver } = useQuery({ queryKey: ['driver'], queryFn: driversService.me })
  const { data: vehicles } = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesService.list })

  const pendingCount = (vehicles ?? []).filter((v) => v.approvalStatus === 'PENDING').length
  const approvedCount = (vehicles ?? []).filter((v) => v.approvalStatus === 'APPROVED').length

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">
          Welcome back{driver ? `, ${driver.firstName}` : ''}
        </h1>
        <p className="mt-1 text-sm text-ink/60">Manage your vehicles and verification from here.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="card p-5">
          <p className="text-xs uppercase tracking-wide text-ink/40">Verification</p>
          <div className="mt-2">{driver && <StatusBadge status={driver.verificationStatus} />}</div>
          <p className="mt-2 text-sm text-ink/60">
            {driver?.verified ? 'You are verified.' : 'Complete your profile and documents for review.'}
          </p>
        </div>
        <div className="card p-5">
          <p className="text-xs uppercase tracking-wide text-ink/40">Vehicles</p>
          <p className="mt-2 font-display text-2xl font-semibold text-forest">{vehicles?.length ?? 0}</p>
          <p className="mt-1 text-sm text-ink/60">
            {pendingCount} pending · {approvedCount} approved
          </p>
        </div>
        <div className="card p-5">
          <p className="text-xs uppercase tracking-wide text-ink/40">Rating</p>
          <p className="mt-2 font-display text-2xl font-semibold text-forest">
            {driver ? driver.ratingAvg.toFixed(1) : '—'}
          </p>
          <p className="mt-1 text-sm text-ink/60">{driver?.ratingCount ?? 0} review(s)</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <Link to="/driver/vehicles/new">
          <Button>
            <Plus className="h-4 w-4" /> Add vehicle
          </Button>
        </Link>
        <Link to="/driver/vehicles">
          <Button variant="outline">
            <Car className="h-4 w-4" /> My vehicles
          </Button>
        </Link>
        <Link to="/driver/profile">
          <Button variant="ghost">
            <ShieldCheck className="h-4 w-4" /> Verification profile
          </Button>
        </Link>
      </div>

      <div className="card p-6">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-forest">Recent vehicles</h2>
          <Link
            to="/driver/vehicles"
            className="inline-flex items-center gap-1 text-sm font-medium text-gold-dark hover:text-gold"
          >
            View all <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        <ul className="mt-4 divide-y divide-line">
          {(vehicles ?? []).slice(0, 5).map((v) => (
            <li key={v.id} className="flex items-center justify-between py-3">
              <Link
                to={`/driver/vehicles/${v.id}`}
                className="font-medium text-forest hover:text-gold-dark"
              >
                {v.make} {v.model} <span className="text-ink/40">· {v.registrationNumber}</span>
              </Link>
              <StatusBadge status={v.approvalStatus} />
            </li>
          ))}
          {vehicles && vehicles.length === 0 && (
            <li className="py-6 text-center text-sm text-ink/50">No vehicles yet.</li>
          )}
        </ul>
      </div>
    </div>
  )
}
