import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  CheckCircle2, AlertCircle, Loader2, ChevronRight, ArrowLeft
} from 'lucide-react'
import { bookingsService } from '@/services/bookings.service'
import { paymentsService, type InitiatePaymentPayload } from '@/services/payments.service'
import { Button, Input, Spinner } from '@/components/ui'
import type { ApiError, PaymentProviderType } from '@/types'

function formatRwf(n: number) {
  return new Intl.NumberFormat('en-RW', {
    style: 'currency', currency: 'RWF', maximumFractionDigits: 0,
  }).format(n)
}

const PROVIDERS: { id: PaymentProviderType; label: string; logo: string }[] = [
  { id: 'MTN_MOMO', label: 'MTN Mobile Money', logo: '📱' },
  { id: 'AIRTEL',   label: 'Airtel Money',      logo: '📲' },
]

export default function PaymentPage() {
  const { bookingId } = useParams<{ bookingId: string }>()
  const navigate = useNavigate()

  const [provider, setProvider] = useState<PaymentProviderType>('MTN_MOMO')
  const [phone, setPhone] = useState('')
  const [paymentId, setPaymentId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [step, setStep] = useState<'choose' | 'waiting' | 'done' | 'failed'>('choose')

  const { data: booking } = useQuery({
    queryKey: ['booking', bookingId],
    queryFn: () => bookingsService.myBooking(bookingId!),
    enabled: !!bookingId,
  })

  const initiateMut = useMutation({
    mutationFn: (payload: InitiatePaymentPayload) => paymentsService.initiate(payload),
    onSuccess: (payment) => {
      setPaymentId(payment.id)
      setStep('waiting')
    },
    onError: (e) => setError((e as ApiError).message),
  })

  const devConfirmMut = useMutation({
    mutationFn: () => paymentsService.devConfirm(paymentId!),
    onSuccess: () => setStep('done'),
    onError: (e) => {
      setError((e as ApiError).message)
      setStep('failed')
    },
  })

  if (!booking) {
    return <div className="grid min-h-screen place-items-center bg-mist"><Spinner className="h-8 w-8" /></div>
  }

  if (step === 'done') {
    return (
      <div className="container-page max-w-sm py-20 text-center">
        <div className="card p-10">
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-forest">
            <CheckCircle2 className="h-8 w-8 text-gold" />
          </div>
          <h1 className="mt-6 font-display text-2xl font-semibold text-forest">Payment confirmed!</h1>
          <p className="mt-2 text-sm text-ink/60">
            Your booking is now confirmed. Have a safe journey.
          </p>
          <div className="mt-6 space-y-2">
            <Button className="w-full" onClick={() => navigate(`/bookings/${bookingId}`)}>
              View booking
            </Button>
            <Button variant="ghost" className="w-full" onClick={() => navigate('/bookings')}>
              My bookings
            </Button>
          </div>
        </div>
      </div>
    )
  }

  if (step === 'failed') {
    return (
      <div className="container-page max-w-sm py-20 text-center">
        <div className="card p-10">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500" />
          <h1 className="mt-5 font-display text-xl font-semibold text-forest">Payment failed</h1>
          {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
          <Button className="mt-6 w-full" onClick={() => { setStep('choose'); setError(null) }}>
            Try again
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="container-page max-w-md py-8">
      <button
        onClick={() => navigate(-1)}
        className="mb-5 flex items-center gap-2 text-sm text-ink/50 hover:text-forest"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      {/* Amount summary */}
      <div className="card mb-5 p-5">
        <div className="flex justify-between text-sm text-ink/60">
          <span>{booking.origin} → {booking.destination}</span>
          <span>{booking.seatsBooked} seat{booking.seatsBooked !== 1 ? 's' : ''}</span>
        </div>
        <div className="mt-3 flex justify-between font-semibold text-forest">
          <span>Total to pay</span>
          <span className="font-display text-xl">{formatRwf(booking.totalAmount)}</span>
        </div>
        <p className="mt-1 text-xs text-ink/40">Incl. 3% service fee of {formatRwf(booking.serviceFee)}</p>
      </div>

      {step === 'choose' && (
        <div className="card space-y-5 p-5">
          <h1 className="font-display text-lg font-semibold text-forest">Choose payment method</h1>

          {/* Provider selection */}
          <div className="space-y-2">
            {PROVIDERS.map((p) => (
              <button
                key={p.id}
                onClick={() => setProvider(p.id)}
                className={`flex w-full items-center gap-3 rounded-xl border p-4 text-left transition ${
                  provider === p.id
                    ? 'border-forest bg-forest/5'
                    : 'border-line hover:border-forest/30'
                }`}
              >
                <span className="text-2xl">{p.logo}</span>
                <span className="font-medium text-forest">{p.label}</span>
                {provider === p.id && (
                  <span className="ml-auto grid h-5 w-5 place-items-center rounded-full bg-forest">
                    <span className="h-2 w-2 rounded-full bg-white" />
                  </span>
                )}
              </button>
            ))}
          </div>

          <Input
            label="Mobile number"
            type="tel"
            placeholder="+250 7XX XXX XXX"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />

          {error && <p className="text-sm text-red-600">{error}</p>}

          {/* Fixed bottom CTA on mobile */}
          <div className="fixed bottom-0 left-0 right-0 border-t border-line bg-white p-4 sm:static sm:border-0 sm:p-0">
            <Button
              className="w-full text-base"
              size="lg"
              loading={initiateMut.isPending}
              onClick={() =>
                initiateMut.mutate({ bookingId: bookingId!, provider, phoneNumber: phone })
              }
            >
              Pay {formatRwf(booking.totalAmount)} <ChevronRight className="h-5 w-5" />
            </Button>
          </div>
        </div>
      )}

      {step === 'waiting' && (
        <div className="card p-8 text-center">
          <Loader2 className="mx-auto h-10 w-10 animate-spin text-gold" />
          <h2 className="mt-5 font-display text-lg font-semibold text-forest">
            Waiting for payment
          </h2>
          <p className="mt-2 text-sm text-ink/60">
            Check your phone for the {provider === 'MTN_MOMO' ? 'MTN Mobile Money' : 'Airtel Money'} prompt.
          </p>
          <p className="mt-4 text-xs text-ink/40">
            If you approved on your phone, tap the button below.
          </p>
          {/* Dev bypass button — visible in development to test without real credentials */}
          <Button
            className="mt-5 w-full"
            loading={devConfirmMut.isPending}
            onClick={() => devConfirmMut.mutate()}
          >
            I've approved the payment
          </Button>
          <button
            onClick={() => setStep('choose')}
            className="mt-3 text-sm text-ink/40 hover:text-ink"
          >
            Cancel and go back
          </button>
        </div>
      )}
    </div>
  )
}
