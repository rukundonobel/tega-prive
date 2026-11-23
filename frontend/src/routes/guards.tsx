import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '@/context/AuthContext'
import type { Role } from '@/types'
import { Spinner } from '@/components/ui'

function CenteredSpinner() {
  return (
    <div className="grid min-h-screen place-items-center bg-mist">
      <Spinner className="h-8 w-8" />
    </div>
  )
}

/** Requires an authenticated user; otherwise redirects to /login. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <CenteredSpinner />
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />
  return <>{children}</>
}

/** Requires the user to hold one of the given roles; otherwise redirects home. */
export function RoleRoute({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <CenteredSpinner />
  if (!user) return <Navigate to="/login" replace />
  const allowed = user.roles.some((r) => roles.includes(r))
  if (!allowed) return <Navigate to="/" replace />
  return <>{children}</>
}
