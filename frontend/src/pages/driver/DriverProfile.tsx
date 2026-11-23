import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Camera, FileUp, ShieldCheck } from 'lucide-react'
import { driversService } from '@/services/drivers.service'
import { useAuth } from '@/context/AuthContext'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Input, Spinner } from '@/components/ui'
import type { ApiError } from '@/types'

const DOC_TYPES = ['IDENTITY', 'LICENCE', 'PROOF_OF_ADDRESS', 'OTHER']

export default function DriverProfile() {
  const { refresh, user } = useAuth()
  const isDriver = !!user?.roles.includes('DRIVER')

  // Only fetch the driver profile once the user actually has the DRIVER role.
  const driverQuery = useQuery({
    queryKey: ['driver'],
    queryFn: driversService.me,
    enabled: isDriver,
    retry: false,
  })
  const docsQuery = useQuery({
    queryKey: ['driver-docs'],
    queryFn: driversService.documents,
    enabled: isDriver && !!driverQuery.data,
    retry: false,
  })

  const driver = driverQuery.data
  const [licenceNumber, setLicenceNumber] = useState('')
  const [licenceExpiry, setLicenceExpiry] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (driver) {
      setLicenceNumber(driver.licenceNumber ?? '')
      setLicenceExpiry(driver.licenceExpiry ?? '')
    }
  }, [driver])

  const registerMut = useMutation({
    mutationFn: () =>
      driversService.register({ licenceNumber, licenceExpiry: licenceExpiry || undefined }),
    onSuccess: async () => {
      await refresh()
      await driverQuery.refetch()
    },
    onError: (e) => setError((e as ApiError).message),
  })

  const updateMut = useMutation({
    mutationFn: () =>
      driversService.update({ licenceNumber, licenceExpiry: licenceExpiry || undefined }),
    onSuccess: () => driverQuery.refetch(),
    onError: (e) => setError((e as ApiError).message),
  })

  const photoMut = useMutation({
    mutationFn: (file: File) => driversService.uploadPhoto(file),
    onSuccess: () => driverQuery.refetch(),
    onError: (e) => alert((e as ApiError).message),
  })

  const docMut = useMutation({
    mutationFn: ({ type, file }: { type: string; file: File }) => driversService.addDocument(type, file),
    onSuccess: () => {
      driverQuery.refetch()
      docsQuery.refetch()
    },
    onError: (e) => alert((e as ApiError).message),
  })

  const submitLicence = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (isDriver) updateMut.mutate()
    else registerMut.mutate()
  }

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">
          {isDriver ? 'Driver profile' : 'Become a driver'}
        </h1>
        <p className="mt-1 text-sm text-ink/60">
          {isDriver
            ? 'Manage your licence, photo and verification documents.'
            : 'Submit your licence details and verification documents to start driving.'}
        </p>
      </div>

      {isDriver && driver && (
        <div className="card flex items-center justify-between p-5">
          <div className="flex items-center gap-3">
            <ShieldCheck className="h-6 w-6 text-gold-dark" />
            <div>
              <p className="text-sm text-ink/60">Verification status</p>
              <div className="mt-1">
                <StatusBadge status={driver.verificationStatus} />
              </div>
            </div>
          </div>
          {driver.rejectionReason && (
            <p className="max-w-xs text-right text-sm text-red-600">{driver.rejectionReason}</p>
          )}
        </div>
      )}

      {error && <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      {/* Licence form (registration for non-drivers, edit for drivers) */}
      <form onSubmit={submitLicence} className="card space-y-4 p-6">
        <h2 className="font-semibold text-forest">Driving licence</h2>
        <Input
          label="Licence number"
          value={licenceNumber}
          onChange={(e) => setLicenceNumber(e.target.value)}
          required
        />
        <Input
          label="Licence expiry"
          type="date"
          value={licenceExpiry}
          onChange={(e) => setLicenceExpiry(e.target.value)}
        />
        <Button type="submit" loading={registerMut.isPending || updateMut.isPending}>
          {isDriver ? 'Save licence' : 'Register as driver'}
        </Button>
      </form>

      {/* Profile photo + documents only after a driver profile exists */}
      {isDriver &&
        (driver ? (
          <>
            <div className="card space-y-4 p-6">
              <h2 className="font-semibold text-forest">Profile photo</h2>
              <div className="flex items-center gap-4">
                <div className="grid h-20 w-20 place-items-center overflow-hidden rounded-2xl bg-mist">
                  {driver.profilePhotoUrl ? (
                    <img src={driver.profilePhotoUrl} alt="Profile" className="h-full w-full object-cover" />
                  ) : (
                    <Camera className="h-7 w-7 text-ink/30" />
                  )}
                </div>
                <label className="cursor-pointer">
                  <span className="btn bg-gold px-5 py-2 text-sm text-forest hover:bg-gold-dark hover:text-white">
                    <FileUp className="h-4 w-4" /> Upload photo
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    disabled={photoMut.isPending}
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      if (f) photoMut.mutate(f)
                    }}
                  />
                </label>
              </div>
            </div>

            <div className="card space-y-4 p-6">
              <h2 className="font-semibold text-forest">Verification documents</h2>
              <ul className="divide-y divide-line">
                {(docsQuery.data ?? []).map((d) => (
                  <li key={d.id} className="flex items-center justify-between py-3">
                    <div>
                      <p className="font-medium text-forest">{d.type}</p>
                      <a href={d.url} target="_blank" rel="noreferrer" className="text-sm text-gold-dark hover:underline">
                        View document
                      </a>
                    </div>
                    <StatusBadge status={d.status} />
                  </li>
                ))}
                {docsQuery.data && docsQuery.data.length === 0 && (
                  <li className="py-4 text-center text-sm text-ink/50">No documents uploaded yet.</li>
                )}
              </ul>

              <div className="flex flex-wrap items-end gap-3 border-t border-line pt-4">
                <div>
                  <label className="label">Document type</label>
                  <select id="docType" className="input-base" defaultValue="IDENTITY">
                    {DOC_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
                <label className="cursor-pointer">
                  <span className="btn border border-forest/20 px-5 py-2 text-sm text-forest hover:bg-forest hover:text-white">
                    <FileUp className="h-4 w-4" /> Choose file
                  </span>
                  <input
                    type="file"
                    accept="image/*,application/pdf"
                    className="hidden"
                    disabled={docMut.isPending}
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      const sel = document.getElementById('docType') as HTMLSelectElement | null
                      if (f && sel) docMut.mutate({ type: sel.value, file: f })
                    }}
                  />
                </label>
              </div>
            </div>
          </>
        ) : (
          <div className="grid place-items-center py-12">
            <Spinner />
          </div>
        ))}
    </div>
  )
}
