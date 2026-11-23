import { Link } from 'react-router-dom'
import { Mail, Phone, MapPin } from 'lucide-react'
import { Logo } from './Logo'

export function Footer() {
  return (
    <footer className="bg-forest text-white/80">
      <div className="container-page py-14">
        <div className="grid gap-10 md:grid-cols-4">
          <div>
            <Logo onLight />
            <p className="mt-4 max-w-xs text-sm text-white/60">
              Your journey. Your freedom. Verified inter-province transport across Rwanda.
            </p>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Platform</h4>
            <ul className="mt-3 space-y-2 text-sm">
              <li>
                <Link to="/search" className="hover:text-gold">
                  Find a ride
                </Link>
              </li>
              <li>
                <Link to="/register" className="hover:text-gold">
                  Become a driver
                </Link>
              </li>
              <li>
                <Link to="/#how" className="hover:text-gold">
                  How it works
                </Link>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Company</h4>
            <ul className="mt-3 space-y-2 text-sm">
              <li>
                <a href="#" className="hover:text-gold">
                  Terms
                </a>
              </li>
              <li>
                <a href="#" className="hover:text-gold">
                  Privacy
                </a>
              </li>
              <li>
                <Link to="/#safety" className="hover:text-gold">
                  Safety
                </Link>
              </li>
            </ul>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Contact</h4>
            <ul className="mt-3 space-y-2 text-sm">
              <li className="flex items-center gap-2">
                <Mail className="h-4 w-4 text-gold" /> hello@tegaprive.rw
              </li>
              <li className="flex items-center gap-2">
                <Phone className="h-4 w-4 text-gold" /> +250 7XX XXX XXX
              </li>
              <li className="flex items-center gap-2">
                <MapPin className="h-4 w-4 text-gold" /> Kigali, Rwanda
              </li>
            </ul>
          </div>
        </div>
        <div className="mt-12 flex flex-col gap-2 border-t border-white/10 pt-6 text-xs text-white/50 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} TEGA PRIVÉ. All rights reserved.</p>
          <p>Registered in Rwanda.</p>
        </div>
      </div>
    </footer>
  )
}
