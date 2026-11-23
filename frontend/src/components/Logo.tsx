import { Link } from 'react-router-dom'

export function Logo({ className, onLight = false }: { className?: string; onLight?: boolean }) {
  const text = onLight ? 'text-white' : 'text-forest'
  return (
    <Link to="/" className={`inline-flex items-center gap-2 ${className ?? ''}`} aria-label="TEGA PRIVÉ home">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gold">
        <svg
          viewBox="0 0 24 24"
          className="h-5 w-5 text-forest"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M4 17l4-9 4 5 3-3 5 7" />
          <circle cx="8" cy="8" r="1.4" fill="currentColor" stroke="none" />
        </svg>
      </span>
      <span className={`font-display text-lg font-semibold tracking-tight ${text}`}>
        TEGA <span className="text-gold">PRIVÉ</span>
      </span>
    </Link>
  )
}
