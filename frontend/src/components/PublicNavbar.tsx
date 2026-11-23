import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Menu, X, ChevronDown, Car, ShieldCheck, LogOut, User as UserIcon, CheckCircle2, Bell } from 'lucide-react'
import { Logo } from './Logo'
import { Button } from './ui'
import { useAuth } from '@/context/AuthContext'
import { useQuery } from '@tanstack/react-query'
import { notificationsService } from '@/services/notifications.service'

const links = [
  { to: '/#how', label: 'How it works' },
  { to: '/#destinations', label: 'Destinations' },
  { to: '/#safety', label: 'Safety' },
  { to: '/#faq', label: 'FAQ' },
]
export function PublicNavbar() {
  const [open, setOpen] = useState(false)
  const [menu, setMenu] = useState(false)
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()

  const { data: unreadCount } = useQuery({
    queryKey: ['unread-count'],
    queryFn: notificationsService.unreadCount,
    enabled: !!user,
    refetchInterval: 60_000,
  })

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-white/80 backdrop-blur supports-[backdrop-filter]:bg-white/70">
      <nav className="container-page flex h-16 items-center justify-between">
        <Logo />

        <div className="hidden items-center gap-8 md:flex">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className="text-sm font-medium text-ink/70 hover:text-forest">
              {l.label}
            </NavLink>
          ))}
        </div>

        <div className="hidden items-center gap-3 md:flex">
          {user ? (
            <div className="flex items-center gap-2">
              {/* Notification bell */}
              <Link to="/notifications" className="relative grid h-9 w-9 place-items-center rounded-xl hover:bg-mist">
                <Bell className="h-5 w-5 text-forest" />
                {!!unreadCount && unreadCount > 0 && (
                  <span className="absolute right-1.5 top-1.5 grid h-4 w-4 place-items-center rounded-full bg-gold text-xs font-bold text-forest">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </Link>
              <div className="relative">
              <button
                onClick={() => setMenu((v) => !v)}
                className="flex items-center gap-2 rounded-xl border border-line px-3 py-2 text-sm font-medium text-forest hover:bg-mist"
              >
                <span className="grid h-6 w-6 place-items-center rounded-full bg-forest text-xs text-white">
                  {user.firstName.charAt(0)}
                </span>
                {user.firstName}
                <ChevronDown className="h-4 w-4" />
              </button>
              {menu && (
                <div
                  className="absolute right-0 mt-2 w-56 overflow-hidden rounded-xl border border-line bg-white shadow-card-hover"
                  onMouseLeave={() => setMenu(false)}
                >
                  {hasRole('DRIVER') && (
                    <Link
                      to="/driver"
                      onClick={() => setMenu(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-sm text-ink/80 hover:bg-mist"
                    >
                      <Car className="h-4 w-4 text-gold-dark" /> Driver dashboard
                    </Link>
                  )}
                  {(hasRole('ADMIN') || hasRole('SUPER_ADMIN')) && (
                    <Link
                      to="/admin"
                      onClick={() => setMenu(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-sm text-ink/80 hover:bg-mist"
                    >
                      <ShieldCheck className="h-4 w-4 text-gold-dark" /> Admin dashboard
                    </Link>
                  )}
                  {!hasRole('DRIVER') && !hasRole('ADMIN') && !hasRole('SUPER_ADMIN') && (
                    <Link
                      to="/driver/profile"
                      onClick={() => setMenu(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-sm text-ink/80 hover:bg-mist"
                    >
                      <UserIcon className="h-4 w-4 text-gold-dark" /> Become a driver
                    </Link>
                  )}
                  <Link
                    to="/bookings"
                    onClick={() => setMenu(false)}
                    className="flex items-center gap-2 px-4 py-2.5 text-sm text-ink/80 hover:bg-mist"
                  >
                    <CheckCircle2 className="h-4 w-4 text-gold-dark" /> My bookings
                  </Link>
                  <button
                    onClick={() => {
                      setMenu(false)
                      logout()
                      navigate('/')
                    }}
                    className="flex w-full items-center gap-2 border-t border-line px-4 py-2.5 text-sm text-ink/80 hover:bg-mist"
                  >
                    <LogOut className="h-4 w-4 text-gold-dark" /> Sign out
                  </button>
                </div>
              )}
            </div>
            </div>
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Sign in
                </Button>
              </Link>
              <Link to="/register">
                <Button size="sm">Get started</Button>
              </Link>
            </>
          )}
        </div>

        <button
          className="-mr-2 p-2 text-forest md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          {open ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </nav>

      {open && (
        <div className="border-t border-line bg-white md:hidden">
          <div className="container-page flex flex-col gap-1 py-3">
            {links.map((l) => (
              <Link
                key={l.to}
                to={l.to}
                onClick={() => setOpen(false)}
                className="rounded-lg px-3 py-2 text-sm text-ink/80 hover:bg-mist"
              >
                {l.label}
              </Link>
            ))}
            <div className="mt-2 flex gap-2">
              {user ? (
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => {
                    setOpen(false)
                    logout()
                    navigate('/')
                  }}
                >
                  Sign out
                </Button>
              ) : (
                <>
                  <Link to="/login" onClick={() => setOpen(false)} className="flex-1">
                    <Button variant="outline" size="sm" className="w-full">
                      Sign in
                    </Button>
                  </Link>
                  <Link to="/register" onClick={() => setOpen(false)} className="flex-1">
                    <Button size="sm" className="w-full">
                      Get started
                    </Button>
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  )
}
