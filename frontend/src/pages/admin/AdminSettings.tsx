import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { Button, Spinner } from '@/components/ui'

export default function AdminSettings() {
  const qc = useQueryClient()
  const { data: settings, isLoading } = useQuery({
    queryKey: ['admin', 'settings'],
    queryFn: adminService.settings,
  })

  const [edited, setEdited] = useState<Record<string, string>>({})

  const update = useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      adminService.updateSetting(key, value),
    onSuccess: () => {
      setEdited({})
      qc.invalidateQueries({ queryKey: ['admin', 'settings'] })
    },
  })

  const handleSave = (key: string) => {
    const value = edited[key]
    if (value !== undefined) update.mutate({ key, value })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Platform settings</h1>
        <p className="mt-1 text-sm text-ink/60">Configure platform-wide behaviour and rules.</p>
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="card divide-y divide-line">
        {(settings ?? []).map((s) => {
          const currentVal = edited[s.key] ?? s.value
          const changed = edited[s.key] !== undefined && edited[s.key] !== s.value
          return (
            <div key={s.key} className="flex flex-wrap items-start gap-4 px-5 py-4">
              <div className="flex-1 min-w-48">
                <p className="font-medium text-forest">{s.key.replace(/_/g, ' ')}</p>
                {s.description && <p className="mt-0.5 text-xs text-ink/50">{s.description}</p>}
              </div>
              <div className="flex items-center gap-2">
                <input
                  className="input-base w-48 text-sm"
                  value={currentVal}
                  onChange={(e) => setEdited((prev) => ({ ...prev, [s.key]: e.target.value }))}
                />
                {changed && (
                  <Button
                    size="sm"
                    loading={update.isPending}
                    onClick={() => handleSave(s.key)}
                  >
                    <Save className="h-4 w-4" /> Save
                  </Button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
