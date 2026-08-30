const STATUS_MAP = {
  COMPLETED: { className: 'badge-verified', label: 'Verified' },
  VERIFIED: { className: 'badge-verified', label: 'Verified' },
  ACTIVE: { className: 'badge-verified', label: 'Active' },
  PENDING: { className: 'badge-pending', label: 'Pending' },
  PROCESSING: { className: 'badge-pending', label: 'Processing' },
  FAILED: { className: 'badge-error', label: 'Failed' },
  REJECTED: { className: 'badge-error', label: 'Rejected' },
  SUBMITTED: { className: 'badge-pending', label: 'Submitted' },
  IN_PROGRESS: { className: 'badge bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-200', label: 'In Progress' },
  RESOLVED: { className: 'badge-verified', label: 'Resolved' },
  CLOSED: { className: 'badge bg-slate-100 text-slate-700 ring-1 ring-inset ring-slate-300', label: 'Closed' },
}

export default function StatusBadge({ status }) {
  const entry = STATUS_MAP[status] || { className: 'badge-pending', label: status }
  return <span className={entry.className}>{entry.label}</span>
}
