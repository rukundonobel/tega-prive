import { useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Search,
  MapPin,
  ShieldCheck,
  Tag,
  Navigation,
  Lock,
  ArrowRight,
  ChevronDown,
  CalendarDays,
  Users,
  Car,
  Star,
  Route,
  CheckCircle2,
  type LucideIcon,
} from 'lucide-react'
import { Button } from '@/components/ui'

const destinations = [
  { name: 'Kigali', region: 'Capital' },
  { name: 'Musanze', region: 'Northern Province' },
  { name: 'Rubavu', region: 'Western Province' },
  { name: 'Huye', region: 'Southern Province' },
  { name: 'Nyagatare', region: 'Eastern Province' },
  { name: 'Rusizi', region: 'Western Province' },
  { name: 'Karongi', region: 'Western Province' },
  { name: 'Muhanga', region: 'Southern Province' },
]

const steps: { icon: LucideIcon; title: string; text: string }[] = [
  { icon: Search, title: 'Search', text: 'Enter your departure, destination, travel date and number of seats.' },
  { icon: CalendarDays, title: 'Book', text: 'Compare verified drivers and vehicles, then reserve your seats securely.' },
  { icon: Navigation, title: 'Travel', text: 'Track your vehicle live and ride with a verified, rated driver.' },
]

const features: { icon: LucideIcon; title: string; text: string }[] = [
  { icon: ShieldCheck, title: 'Verified drivers', text: 'Every driver passes identity, licence and vehicle document checks before approval.' },
  { icon: Tag, title: 'Transparent pricing', text: 'See the seat price and service fee up front. No surprises at pickup.' },
  { icon: Navigation, title: 'Live tracking', text: 'Follow your vehicle in real time once your booking is confirmed.' },
  { icon: Lock, title: 'Secure payments', text: 'Pay with Mobile Money. Payments are verified on our secure backend.' },
]

const faqs = [
  { q: 'Do I need an account to search for rides?', a: 'No. You can search available journeys freely. You only need an account to complete a booking.' },
  { q: 'How are drivers verified?', a: 'Drivers submit identity documents and their driving licence, and register vehicles with registration and insurance documents. Administrators review and approve each driver and vehicle before they can publish journeys.' },
  { q: 'What payment methods are supported?', a: 'MTN Mobile Money and Airtel Money are supported through a secure payment gateway. Cash bookings may be enabled by administrators.' },
  { q: 'Can I track my vehicle?', a: 'Yes. Once your booking is confirmed, you can follow your vehicle live on the map during the journey.' },
  { q: 'How are prices determined?', a: 'Drivers set the price per seat. TEGA PRIVÉ adds a configurable passenger service fee, and drivers pay a commission. All figures are shown transparently.' },
]

function SectionHeading({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle?: string }) {
  return (
    <div className="max-w-2xl">
      <span className="eyebrow">{eyebrow}</span>
      <h2 className="mt-3 font-display text-3xl font-semibold text-forest sm:text-4xl">{title}</h2>
      {subtitle && <p className="mt-3 text-ink/60">{subtitle}</p>}
    </div>
  )
}

function HeroField({ icon: Icon, label, children }: { icon: LucideIcon; label: string; children: ReactNode }) {
  return (
    <div>
      <label className="label text-ink/70">{label}</label>
      <div className="flex items-center gap-2 rounded-xl border border-line bg-white px-3 focus-within:border-gold focus-within:ring-2 focus-within:ring-gold/30">
        <Icon className="h-4 w-4 shrink-0 text-gold-dark" />
        {children}
      </div>
    </div>
  )
}

export default function Home() {
  const navigate = useNavigate()
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [date, setDate] = useState('')
  const [passengers, setPassengers] = useState('1')
  const [openFaq, setOpenFaq] = useState<number | null>(0)

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    const params = new URLSearchParams()
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    if (date) params.set('date', date)
    if (passengers && passengers !== '1') params.set('passengers', passengers)
    navigate(`/search?${params.toString()}`)
  }

  return (
    <div>
      {/* ===== HERO ===== */}
      <section className="relative overflow-hidden bg-forest text-white">
        <svg className="pointer-events-none absolute inset-0 h-full w-full opacity-[0.08]" aria-hidden="true">
          <defs>
            <pattern id="hero-grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M40 0H0V40" fill="none" stroke="#C5A46D" strokeWidth="1" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#hero-grid)" />
        </svg>
        <div className="container-page relative py-20 sm:py-28">
          <span className="eyebrow text-gold">Your journey. Your freedom.</span>
          <h1 className="mt-5 max-w-2xl font-display text-4xl font-semibold leading-tight sm:text-5xl">
            Travel between Rwanda's provinces with verified drivers.
          </h1>
          <p className="mt-5 max-w-xl text-white/70">
            Book a seat with trusted, rated drivers travelling from Kigali to Musanze, Rubavu, Huye and beyond.
            Search, book, pay and track — all in one place.
          </p>

          <form onSubmit={onSearch} className="mt-10 max-w-5xl rounded-2xl bg-white p-4 text-ink shadow-card-hover sm:p-5">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <HeroField icon={MapPin} label="From">
                <input
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                  placeholder="Kigali"
                  className="h-11 w-full bg-transparent outline-none placeholder:text-ink/40"
                />
              </HeroField>
              <HeroField icon={MapPin} label="To">
                <input
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                  placeholder="Musanze"
                  className="h-11 w-full bg-transparent outline-none placeholder:text-ink/40"
                />
              </HeroField>
              <HeroField icon={CalendarDays} label="Date">
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="h-11 w-full bg-transparent outline-none"
                />
              </HeroField>
              <HeroField icon={Users} label="Passengers">
                <select
                  value={passengers}
                  onChange={(e) => setPassengers(e.target.value)}
                  className="h-11 w-full bg-transparent outline-none"
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                    <option key={n} value={n}>
                      {n} {n === 1 ? 'passenger' : 'passengers'}
                    </option>
                  ))}
                </select>
              </HeroField>
            </div>
            <Button type="submit" size="lg" className="mt-4 w-full sm:w-auto">
              <Search className="h-4 w-4" /> Search rides
            </Button>
          </form>
        </div>
      </section>

      {/* ===== HOW IT WORKS ===== */}
      <section id="how" className="container-page py-20">
        <SectionHeading eyebrow="How it works" title="Three steps to your next journey" />
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {steps.map((s, i) => (
            <div key={s.title} className="card p-7">
              <div className="flex items-center gap-3">
                <span className="grid h-11 w-11 place-items-center rounded-xl bg-forest text-gold">
                  <s.icon className="h-5 w-5" />
                </span>
                <span className="font-display text-3xl font-semibold text-gold/30">0{i + 1}</span>
              </div>
              <h3 className="mt-5 text-lg font-semibold text-forest">{s.title}</h3>
              <p className="mt-2 text-sm text-ink/60">{s.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ===== DESTINATIONS ===== */}
      <section id="destinations" className="bg-mist py-20">
        <div className="container-page">
          <SectionHeading
            eyebrow="Popular destinations"
            title="Where TEGA PRIVÉ can take you"
            subtitle="Routes across Rwanda, managed by our team — with more added over time."
          />
          <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {destinations.map((d) => (
              <div
                key={d.name}
                className="group overflow-hidden rounded-2xl border border-line bg-white shadow-card transition hover:shadow-card-hover"
              >
                <div className="relative h-28 bg-gradient-to-br from-forest to-forest-light">
                  <span className="absolute right-3 top-3">
                    <MapPin className="h-5 w-5 text-gold" />
                  </span>
                </div>
                <div className="p-5">
                  <h3 className="font-semibold text-forest">{d.name}</h3>
                  <p className="mt-1 text-sm text-ink/60">{d.region}</p>
                  <button
                    onClick={() => navigate(`/search?to=${encodeURIComponent(d.name)}`)}
                    className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-gold-dark hover:text-gold"
                  >
                    View rides <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ===== WHY CHOOSE ===== */}
      <section className="container-page py-20">
        <SectionHeading eyebrow="Why TEGA PRIVÉ" title="Built for trust, comfort and freedom" />
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f) => (
            <div key={f.title} className="card p-7">
              <span className="grid h-12 w-12 place-items-center rounded-xl bg-gold/15 text-gold-dark">
                <f.icon className="h-6 w-6" />
              </span>
              <h3 className="mt-5 font-semibold text-forest">{f.title}</h3>
              <p className="mt-2 text-sm text-ink/60">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ===== SAFETY ===== */}
      <section id="safety" className="bg-forest py-20 text-white">
        <div className="container-page grid items-center gap-12 lg:grid-cols-2">
          <div>
            <span className="eyebrow text-gold">Safety first</span>
            <h2 className="mt-3 font-display text-3xl font-semibold sm:text-4xl">
              Every journey starts with verification
            </h2>
            <p className="mt-4 text-white/70">
              We don't allow unverified drivers or unapproved vehicles on the road. Administrators review
              documents and approve each driver and vehicle before any journey is published.
            </p>
            <ul className="mt-6 space-y-3">
              {[
                'Identity and licence verification for every driver',
                'Vehicle registration and insurance document review',
                'Passenger ratings after every completed journey',
                'Live tracking for confirmed bookings',
              ].map((t) => (
                <li key={t} className="flex items-start gap-3 text-white/80">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-gold" />
                  <span>{t}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="rounded-2xl border border-white/10 bg-white/5 p-8 backdrop-blur">
            <ShieldCheck className="h-10 w-10 text-gold" />
            <p className="mt-4 font-display text-2xl">A marketplace you can trust</p>
            <p className="mt-2 text-sm text-white/60">
              Verification, approval and ratings are built into the platform — not optional extras.
            </p>
          </div>
        </div>
      </section>

      {/* ===== BECOME A DRIVER ===== */}
      <section id="drivers" className="container-page py-20">
        <div className="overflow-hidden rounded-3xl border border-line bg-mist p-8 sm:p-12">
          <div className="grid items-center gap-10 lg:grid-cols-2">
            <div>
              <span className="eyebrow">Become a driver</span>
              <h2 className="mt-3 font-display text-3xl font-semibold text-forest sm:text-4xl">
                Turn your vehicle into income
              </h2>
              <p className="mt-4 text-ink/60">
                Register, verify your identity and vehicle, get approved, and publish journeys on your schedule.
                Track your earnings and ratings in one dashboard.
              </p>
              <div className="mt-6">
                <Button onClick={() => navigate('/register')} size="lg">
                  Get started <ArrowRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { icon: Car, label: 'Register your vehicle' },
                { icon: ShieldCheck, label: 'Get verified & approved' },
                { icon: Route, label: 'Publish your journeys' },
                { icon: Star, label: 'Earn & get rated' },
              ].map((b) => (
                <div key={b.label} className="rounded-2xl bg-white p-5 shadow-card">
                  <b.icon className="h-6 w-6 text-gold-dark" />
                  <p className="mt-3 text-sm font-medium text-forest">{b.label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ===== FAQ ===== */}
      <section id="faq" className="container-page py-20">
        <SectionHeading eyebrow="FAQ" title="Questions, answered" />
        <div className="mt-10 max-w-3xl divide-y divide-line rounded-2xl border border-line bg-white">
          {faqs.map((f, i) => {
            const open = openFaq === i
            return (
              <div key={f.q}>
                <button
                  onClick={() => setOpenFaq(open ? null : i)}
                  className="flex w-full items-center justify-between gap-4 p-5 text-left"
                >
                  <span className="font-medium text-forest">{f.q}</span>
                  <ChevronDown
                    className={`h-5 w-5 shrink-0 text-gold-dark transition ${open ? 'rotate-180' : ''}`}
                  />
                </button>
                {open && <p className="px-5 pb-5 text-sm text-ink/60">{f.a}</p>}
              </div>
            )
          })}
        </div>
      </section>
    </div>
  )
}
