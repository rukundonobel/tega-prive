import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Car, User, LayoutDashboard, LogOut, ExternalLink, Route, TrendingUp } from 'lucide-react'
import { Logo } from '@/components/Logo'
import { Button } from '@/components/ui'
import { useAuth } from '@/context/AuthContext'

const nav = [
  { to: '/driver', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/driver/profile', label: 'My profile', icon: User, end: false },
  { to: '/driver/vehicles', label: 'My vehicles', icon: Car, end: false },
  { to: '/driver/journeys', label: 'My journeys', icon: Route, end: false },
  { to: '/driver/earnings', label: 'Earnings', icon: TrendingUp, end: false },
]

export default function DriverLayout() {
  const { logout, user } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="min-h-screen bg-mist">
      <div className="mx-auto flex max-w-7xl">
        <aside className="hidden w-64 shrink-0 border-r border-line bg-white md:block">
          <div className="px-5 py-6">
            <Logo />
          </div>
          <nav className="px-3 pb-6">
            <p className="px-3 pb-2 text-xs font-semibold uppercase tracking-wider text-ink/40">
              Driver portal
            </p>
            {nav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                    isActive ? 'bg-forest text-white' : 'text-ink/70 hover:bg-mist'
                  }`
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="mt-auto px-3">
            <button
              onClick={() => navigate('/')}
              className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-ink/60 hover:bg-mist"
            >
              <ExternalLink className="h-4 w-4" /> View public site
            </button>
            <button
              onClick={() => {
                logout()
                navigate('/')
              }}
              className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-ink/60 hover:bg-mist"
            >
              <LogOut className="h-4 w-4" /> Sign out
            </button>
          </div>
        </aside>

        <main className="min-w-0 flex-1">
          {/* Mobile top bar */}
          <div className="flex items-center justify-between border-b border-line bg-white px-4 py-3 md:hidden">
            <Logo />
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                logout()
                navigate('/')
              }}
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
          <div className="px-4 py-8 sm:px-8">
            <div className="mb-6 text-sm text-ink/50">
              Signed in as <span className="font-medium text-forest">{user?.email}</span>
            </div>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
