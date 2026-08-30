export default function ComplaintStats({ complaints = [], onTabChange }) {
  const total = complaints.length
  const submitted = complaints.filter((c) => c.status === 'SUBMITTED').length
  const inProgress = complaints.filter((c) => c.status === 'IN_PROGRESS').length
  const resolved = complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length

  const stats = [
    {
      id: 'total',
      label: 'Total Complaints',
      count: total,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" className="text-navy-700" aria-hidden="true">
          <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-navy-50',
      border: 'border-navy-200',
    },
    {
      id: 'submitted',
      label: 'Submitted / Review',
      count: submitted,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" className="text-amber-700" aria-hidden="true">
          <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
          <path d="M12 7v5l3 3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-amber-50',
      border: 'border-amber-200',
    },
    {
      id: 'in_progress',
      label: 'In Progress',
      count: inProgress,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" className="text-blue-700" aria-hidden="true">
          <path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-blue-50',
      border: 'border-blue-200',
    },
    {
      id: 'resolved',
      label: 'Resolved',
      count: resolved,
      icon: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" className="text-teal-700" aria-hidden="true">
          <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-teal-50',
      border: 'border-teal-200',
    },
  ]

  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {stats.map((stat) => (
        <button
          key={stat.id}
          type="button"
          onClick={() => onTabChange?.('list')}
          className="card group flex flex-col justify-between p-4 text-left transition-all duration-150 hover:border-slate-300 hover:shadow-elevated"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              {stat.label}
            </span>
            <div className={`flex h-9 w-9 items-center justify-center rounded-lg ${stat.bg} ${stat.border} border`}>
              {stat.icon}
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="font-display text-2xl font-bold text-navy-900">{stat.count}</span>
            <span className="text-xs text-slate-400 group-hover:text-teal-700">view &rarr;</span>
          </div>
        </button>
      ))}
    </div>
  )
}

