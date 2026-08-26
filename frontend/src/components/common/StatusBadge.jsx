const STATUS_MAP = {
  COMPLETED: { className: 'badge-verified', label: 'Verified' },
  VERIFIED: { className: 'badge-verified', label: 'Verified' },
  ACTIVE: { className: 'badge-verified', label: 'Active' },
  PENDING: { className: 'badge-pending', label: 'Pending' },
  PROCESSING: { className: 'badge-pending', label: 'Processing' },
  FAILED: { className: 'badge-error', label: 'Failed' },
  REJECTED: { className: 'badge-error', label: 'Rejected' },
}

export default function StatusBadge({ status }) {
  const entry = STATUS_MAP[status] || { className: 'badge-pending', label: status }
  return <span className={entry.className}>{entry.label}</span>
}
