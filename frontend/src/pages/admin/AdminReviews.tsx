import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Star, Eye, EyeOff } from 'lucide-react'
import { adminService } from '@/services/admin.service'
import { Button, Spinner } from '@/components/ui'

export default function AdminReviews() {
  const qc = useQueryClient()
  const { data: reviews, isLoading } = useQuery({
    queryKey: ['admin', 'reviews'],
    queryFn: adminService.allReviews,
  })

  const hide = useMutation({
    mutationFn: (id: string) => adminService.hideReview(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reviews'] }),
  })
  const show = useMutation({
    mutationFn: (id: string) => adminService.showReview(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'reviews'] }),
  })

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-semibold text-forest">Ratings & reviews</h1>
        <p className="mt-1 text-sm text-ink/60">Moderate passenger reviews of drivers.</p>
      </div>

      {isLoading && <div className="grid place-items-center py-16"><Spinner /></div>}

      <div className="space-y-3">
        {(reviews ?? []).map((r) => (
          <div key={r.id} className={`card p-5 ${r.hidden ? 'opacity-50' : ''}`}>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium text-forest">{r.reviewerName}</p>
                <p className="text-xs text-ink/50">→ {r.driverName}</p>
              </div>
              <div className="flex items-center gap-1">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star key={i} className={`h-4 w-4 ${i < r.rating ? 'fill-gold text-gold' : 'text-line'}`} />
                ))}
              </div>
            </div>
            {r.comment && <p className="mt-2 text-sm text-ink/70">{r.comment}</p>}
            <div className="mt-3 flex items-center gap-2">
              {r.hidden ? (
                <span className="text-xs text-ink/40">Hidden from public</span>
              ) : null}
              {r.hidden ? (
                <Button size="sm" variant="outline" onClick={() => show.mutate(r.id)}>
                  <Eye className="h-4 w-4" /> Show
                </Button>
              ) : (
                <Button size="sm" variant="outline" onClick={() => hide.mutate(r.id)}>
                  <EyeOff className="h-4 w-4" /> Hide
                </Button>
              )}
            </div>
          </div>
        ))}
        {reviews && reviews.length === 0 && (
          <div className="card p-12 text-center text-sm text-ink/50">No reviews yet.</div>
        )}
      </div>
    </div>
  )
}
