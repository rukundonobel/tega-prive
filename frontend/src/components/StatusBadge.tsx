import { Badge } from './ui'

const MAP: Record<string, { tone: 'gold' | 'green' | 'neutral' | 'red'; label: string }> = {
  PENDING: { tone: 'gold', label: 'Pending' },
  APPROVED: { tone: 'green', label: 'Approved' },
  REJECTED: { tone: 'red', label: 'Rejected' },
  DRAFT: { tone: 'neutral', label: 'Draft' },
  ACTIVE: { tone: 'green', label: 'Active' },
  SCHEDULED: { tone: 'gold', label: 'Scheduled' },
  COMPLETED: { tone: 'green', label: 'Completed' },
  CANCELLED: { tone: 'red', label: 'Cancelled' },
  SUSPENDED: { tone: 'red', label: 'Suspended' },
  PENDING_VERIFICATION: { tone: 'gold', label: 'Pending verification' },
  PENDING_PAYMENT: { tone: 'gold', label: 'Awaiting payment' },
  PENDING_DRIVER_APPROVAL: { tone: 'gold', label: 'Awaiting driver' },
  CONFIRMED: { tone: 'green', label: 'Confirmed' },
  REFUNDED: { tone: 'neutral', label: 'Refunded' },
  EXPIRED: { tone: 'neutral', label: 'Expired' },
}

export function StatusBadge({ status }: { status: string }) {
  const entry = MAP[status] ?? { tone: 'neutral' as const, label: status }
  return <Badge tone={entry.tone}>{entry.label}</Badge>
}
