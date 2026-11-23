import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, ShieldCheck, Car, LogOut, ExternalLink,
  Users, BookOpen, TrendingUp, Settings, MessageSquare,
  Star, Menu, X, Navigation, DollarSign
} from 'lucide-react'
import { Logo } from '@/components/Logo'
import { useAuth } from '@/context/AuthContext'

const nav = [
  { to: '/admin',             label: 'Overview',          icon: LayoutDashboard, end: true },
  { to: '/admin/drivers',     label: 'Driver verification',icon: ShieldCheck,    end: false },
  { to: '/admin/vehicles',    label: 'Vehicle approval',  icon: Car,             end: false },
  { to: '/admin/users',       label: 'Users',             icon: Users,           end: false },
  { to: '/admin/bookings',    label: 'Bookings',          icon: BookOpen,        end: false },
  { to: '/admin/monitoring',  label: 'Live monitoring',   icon: Navigation,      end: false },
  { to: '/admin/payments',    label: 'Payments & payouts',icon: DollarSign,      end: false },
  { to: '/admin/reports',     label: 'Revenue reports',   icon: TrendingUp,      end: false },
  { to: '/admin/complaints',  label: 'Complaints',        icon: MessageSquare,   end: false },
  { to: '/admin/reviews',     label: 'Reviews',           icon: Star,            end: false },
  { to: '/admin/settings',    label: 'Platform settings', icon: Settings,        end: false },
]

function NavItem({ item }: { item: (typeof nav)[0] }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
          isActive ? 'bg-gold text-forest' : 'text-white/80 hover:bg-white/10'
        }`
      }
    >
      <item.icon className="h-4 w-4 shrink-0" />
      {item.label}
    </NavLink>
  )
}

export default function AdminLayout() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-mist">
      {/* Mobile header */}
      <div className="flex items-center justify-between border-b border-line bg-forest px-4 py-3 md:hidden">
        <Logo onLight />
        <button
          onClick={() => setMobileOpen((v) => !v)}
          className="text-white"
        >
          {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </div>

      {/* Mobile nav drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <div className="w-72 bg-forest py-4 px-3 overflow-y-auto">
            <div className="mb-4 px-3">
              <Logo onLight />
            </div>
            <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-white/40">Admin</p>
            {nav.map((item) => (
              <div key={item.to} onClick={() => setMobileOpen(false)}>
                <NavItem item={item} />
              </div>
            ))}
            <div className="mt-4 border-t border-white/10 pt-4">
              <button
                onClick={() => { setMobileOpen(false); navigate('/') }}
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-white/60 hover:bg-white/10"
              >
                <ExternalLink className="h-4 w-4" /> View public site
              </button>
              <button
                onClick={handleLogout}
                className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-white/60 hover:bg-white/10"
              >
                <LogOut className="h-4 w-4" /> Sign out
              </button>
            </div>
          </div>
          <div className="flex-1 bg-black/50" onClick={() => setMobileOpen(false)} />
        </div>
      )}

      <div className="mx-auto flex max-w-7xl">
        {/* Desktop sidebar */}
        <aside className="hidden w-64 shrink-0 md:block">
          <div className="sticky top-0 h-screen overflow-y-auto bg-forest py-6">
            <div className="px-5 pb-4">
              <Logo onLight />
            </div>
            <nav className="px-3 pb-6">
              <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-white/40">Admin</p>
              {nav.map((item) => (
                <NavItem key={item.to} item={item} />
              ))}
            </nav>
            <div className="px-3 border-t border-white/10 pt-4">
              <button
                onClick={() => navigate('/')}
                className="flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-white/60 hover:bg-white/10"
              >
                <ExternalLink className="h-4 w-4" /> View public site
              </button>
              <button
                onClick={handleLogout}
                className="mt-1 flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-white/60 hover:bg-white/10"
              >
                <LogOut className="h-4 w-4" /> Sign out
              </button>
            </div>
          </div>
        </aside>

        <main className="min-w-0 flex-1 px-4 py-8 sm:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
