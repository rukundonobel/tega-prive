import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Car, Plus } from 'lucide-react'
import { vehiclesService } from '@/services/vehicles.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Spinner } from '@/components/ui'

export default function Vehicles() {
  const { data: vehicles, isLoading } = useQuery({
    queryKey: ['vehicles'],
    queryFn: vehiclesService.list,
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-semibold text-forest">My vehicles</h1>
          <p className="mt-1 text-sm text-ink/60">
            Add vehicles, upload photos and documents, then submit for approval.
          </p>
        </div>
        <Link to="/driver/vehicles/new">
          <Button>
            <Plus className="h-4 w-4" /> Add vehicle
          </Button>
        </Link>
      </div>

      {isLoading && (
        <div className="grid place-items-center py-16">
          <Spinner />
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {(vehicles ?? []).map((v) => (
          <Link
            key={v.id}
            to={`/driver/vehicles/${v.id}`}
            className="card overflow-hidden transition hover:shadow-card-hover"
          >
            <div className="aspect-[4/3] bg-mist">
              {v.photos[0] ? (
                <img
                  src={v.photos[0].url}
                  alt={`${v.make} ${v.model}`}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="grid h-full place-items-center text-ink/30">
                  <Car className="h-8 w-8" />
                </div>
              )}
            </div>
            <div className="p-4">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-forest">
                  {v.make} {v.model}
                </h3>
                <StatusBadge status={v.approvalStatus} />
              </div>
              <p className="mt-1 text-sm text-ink/60">
                {v.registrationNumber} · {v.seatCapacity} seats
              </p>
            </div>
          </Link>
        ))}
      </div>

      {vehicles && vehicles.length === 0 && (
        <div className="card p-12 text-center">
          <Car className="mx-auto h-10 w-10 text-ink/20" />
          <p className="mt-4 font-medium text-forest">No vehicles yet</p>
          <p className="mt-1 text-sm text-ink/50">Add your first vehicle to get started.</p>
          <Link to="/driver/vehicles/new" className="mt-5 inline-block">
            <Button>
              <Plus className="h-4 w-4" /> Add vehicle
            </Button>
          </Link>
        </div>
      )}
    </div>
  )
}
