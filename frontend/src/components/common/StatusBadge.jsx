const STATUS_MAP = {
  COMPLETED: {
    className: 'bg-emerald-50 text-emerald-800 ring-1 ring-inset ring-emerald-300/80',
    dot: 'bg-emerald-600',
    label: 'Verified',
  },
  VERIFIED: {
    className: 'bg-emerald-50 text-emerald-800 ring-1 ring-inset ring-emerald-300/80',
    dot: 'bg-emerald-600',
    label: 'Verified',
  },
  ACTIVE: {
    className: 'bg-emerald-50 text-emerald-800 ring-1 ring-inset ring-emerald-300/80',
    dot: 'bg-emerald-600',
    label: 'Active',
  },
  PENDING: {
    className: 'bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-300/80',
    dot: 'bg-amber-500 animate-pulse',
    label: 'Pending',
  },
  PROCESSING: {
    className: 'bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-300/80',
    dot: 'bg-amber-500 animate-pulse',
    label: 'Processing',
  },
  FAILED: {
    className: 'bg-red-50 text-red-800 ring-1 ring-inset ring-red-300/80',
    dot: 'bg-red-600',
    label: 'Failed',
  },
  REJECTED: {
    className: 'bg-red-50 text-red-800 ring-1 ring-inset ring-red-300/80',
    dot: 'bg-red-600',
    label: 'Rejected',
  },
  SUBMITTED: {
    className: 'bg-amber-50 text-amber-800 ring-1 ring-inset ring-amber-300/80',
    dot: 'bg-amber-500 animate-pulse',
    label: 'Under Review',
  },
  IN_PROGRESS: {
    className: 'bg-blue-50 text-blue-800 ring-1 ring-inset ring-blue-300/80',
    dot: 'bg-blue-600 animate-pulse',
    label: 'In Action',
  },
  RESOLVED: {
    className: 'bg-teal-50 text-teal-800 ring-1 ring-inset ring-teal-300/80',
    dot: 'bg-teal-600',
    label: 'Resolved',
  },
  CLOSED: {
    className: 'bg-slate-100 text-slate-700 ring-1 ring-inset ring-slate-300/80',
    dot: 'bg-slate-500',
    label: 'Closed',
  },
}

export default function StatusBadge({ status }) {
  const entry = STATUS_MAP[status] || {
    className: 'bg-slate-100 text-slate-700 ring-1 ring-inset ring-slate-300',
    dot: 'bg-slate-400',
    label: status,
  }

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${entry.className}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${entry.dot}`} />
      <span>{entry.label}</span>
    </span>
  )
}

