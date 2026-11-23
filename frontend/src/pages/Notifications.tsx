import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck } from 'lucide-react'
import { notificationsService } from '@/services/notifications.service'
import { Button, Spinner } from '@/components/ui'

function formatDate(iso: string) {
  return new Date(iso).toLocaleString([], {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function Notifications() {
  const qc = useQueryClient()
  const { data: list, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: notificationsService.list,
  })

  const markRead = useMutation({
    mutationFn: notificationsService.markAllRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] })
      qc.invalidateQueries({ queryKey: ['unread-count'] })
    },
  })

  const unread = (list ?? []).filter((n) => n.unread).length

  return (
    <div className="container-page max-w-2xl py-8">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <div>
          <h1 className="font-display text-2xl font-semibold text-forest">Notifications</h1>
          <p className="mt-1 text-sm text-ink/60">{unread} unread</p>
        </div>
        {unread > 0 && (
          <Button size="sm" variant="outline" loading={markRead.isPending} onClick={() => markRead.mutate()}>
            <CheckCheck className="h-4 w-4" /> Mark all read
          </Button>
        )}
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="space-y-2">
        {(list ?? []).map((n) => (
          <div
            key={n.id}
            className={`card p-5 ${n.unread ? 'border-gold/40 bg-gold/5' : ''}`}
          >
            <div className="flex items-start gap-3">
              <div className={`mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-xl ${
                n.unread ? 'bg-gold text-forest' : 'bg-mist text-ink/40'
              }`}>
                <Bell className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <p className={`font-medium ${n.unread ? 'text-forest' : 'text-ink/70'}`}>{n.title}</p>
                <p className="mt-0.5 text-sm text-ink/60">{n.body}</p>
                <p className="mt-1 text-xs text-ink/40">{formatDate(n.createdAt)}</p>
              </div>
            </div>
          </div>
        ))}
        {list && list.length === 0 && (
          <div className="card p-12 text-center">
            <Bell className="mx-auto h-10 w-10 text-ink/20" />
            <p className="mt-4 font-medium text-forest">No notifications yet</p>
          </div>
        )}
      </div>
    </div>
  )
}
