import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search, UserCheck, UserX } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { StatusBadge } from '@/components/StatusBadge'
import { Button, Spinner } from '@/components/ui'

export default function AdminUsers() {
  const qc = useQueryClient()
  const [q, setQ] = useState('')
  const { data: users, isLoading } = useQuery({ queryKey: ['admin', 'users'], queryFn: adminService.allUsers })

  const suspend = useMutation({
    mutationFn: (id: string) => adminService.suspend(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
  const activate = useMutation({
    mutationFn: (id: string) => adminService.activate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })

  const filtered = (users ?? []).filter((u) =>
    !q || u.name.toLowerCase().includes(q.toLowerCase()) || u.email.toLowerCase().includes(q.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">User management</h1>
        <p className="mt-1 text-sm text-ink/60">View, suspend and reactivate platform users.</p>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/30" />
        <input
          className="input-base pl-9"
          placeholder="Search by name or email…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-mist text-left text-xs uppercase tracking-wide text-ink/40">
              <tr>
                <th className="px-5 py-3">Name</th>
                <th className="px-5 py-3">Email</th>
                <th className="px-5 py-3">Roles</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {filtered.map((u) => (
                <tr key={u.id} className="hover:bg-mist/50">
                  <td className="px-5 py-3 font-medium text-forest">{u.name}</td>
                  <td className="px-5 py-3 text-ink/60">{u.email}</td>
                  <td className="px-5 py-3">
                    <div className="flex flex-wrap gap-1">
                      {u.roles.map((r) => (
                        <span key={r} className="rounded-full bg-forest/10 px-2 py-0.5 text-xs text-forest">{r}</span>
                      ))}
                    </div>
                  </td>
                  <td className="px-5 py-3"><StatusBadge status={u.status} /></td>
                  <td className="px-5 py-3">
                    {u.status === 'ACTIVE' ? (
                      <Button size="sm" variant="outline" onClick={() => suspend.mutate(u.id)}>
                        <UserX className="h-4 w-4" /> Suspend
                      </Button>
                    ) : u.status === 'SUSPENDED' ? (
                      <Button size="sm" onClick={() => activate.mutate(u.id)}>
                        <UserCheck className="h-4 w-4" /> Activate
                      </Button>
                    ) : null}
                  </td>
                </tr>
              ))}
              {!filtered.length && (
                <tr><td colSpan={5} className="px-5 py-10 text-center text-ink/40">No users found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
