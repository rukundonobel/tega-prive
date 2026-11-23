import { Link } from 'react-router-dom'
import { Button } from '@/components/ui'

export default function NotFound() {
  return (
    <div className="grid min-h-screen place-items-center bg-mist px-4">
      <div className="text-center">
        <p className="font-display text-6xl font-semibold text-forest">404</p>
        <p className="mt-3 text-ink/60">This page doesn't exist.</p>
        <Link to="/" className="mt-6 inline-block">
          <Button>Back home</Button>
        </Link>
      </div>
    </div>
  )
}
