/**
 * ComplaintStats - Mobile-first responsive metric cards.
 * Displays key complaint statistics with interactive filter triggers.
 */
export default function ComplaintStats({ complaints = [], onTabChange, activeFilter = 'ALL', onFilterChange }) {
  const total = complaints.length
  const submitted = complaints.filter((c) => c.status === 'SUBMITTED').length
  const inProgress = complaints.filter((c) => c.status === 'IN_PROGRESS').length
  const resolved = complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length

  const stats = [
    {
      id: 'ALL',
      label: 'Total Reports',
      count: total,
      sublabel: 'All time submitted',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" className="text-navy-700" aria-hidden="true">
          <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-navy-50',
      badgeColor: 'bg-navy-100 text-navy-800',
      borderColor: 'border-navy-200 hover:border-navy-400',
      activeRing: 'ring-2 ring-navy-800 border-navy-800 shadow-sm',
    },
    {
      id: 'SUBMITTED',
      label: 'Under Review',
      count: submitted,
      sublabel: 'Awaiting inspection',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" className="text-amber-700" aria-hidden="true">
          <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
          <path d="M12 7v5l3 3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-amber-50',
      badgeColor: 'bg-amber-100 text-amber-800',
      borderColor: 'border-amber-200 hover:border-amber-400',
      activeRing: 'ring-2 ring-amber-600 border-amber-600 shadow-sm',
    },
    {
      id: 'IN_PROGRESS',
      label: 'In Action',
      count: inProgress,
      sublabel: 'Ground team active',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" className="text-blue-700" aria-hidden="true">
          <path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-blue-50',
      badgeColor: 'bg-blue-100 text-blue-800',
      borderColor: 'border-blue-200 hover:border-blue-400',
      activeRing: 'ring-2 ring-blue-600 border-blue-600 shadow-sm',
    },
    {
      id: 'RESOLVED',
      label: 'Resolved',
      count: resolved,
      sublabel: 'Verified fixed',
      icon: (
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" className="text-teal-700" aria-hidden="true">
          <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      ),
      bg: 'bg-teal-50',
      badgeColor: 'bg-teal-100 text-teal-800',
      borderColor: 'border-teal-200 hover:border-teal-400',
      activeRing: 'ring-2 ring-teal-600 border-teal-600 shadow-sm',
    },
  ]

  function handleClick(filterId) {
    onTabChange?.('list')
    onFilterChange?.(filterId)
  }

  return (
    <section aria-label="Complaint Summary Statistics">
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        {stats.map((stat) => {
          const isActive = activeFilter === stat.id

          return (
            <button
              key={stat.id}
              type="button"
              onClick={() => handleClick(stat.id)}
              className={`group relative flex flex-col justify-between rounded-xl border bg-white p-3.5 sm:p-4 text-left transition-all duration-200 hover:shadow-elevated active:scale-[0.98] ${
                stat.borderColor
              } ${isActive ? stat.activeRing : 'shadow-card'}`}
            >
              <div className="flex items-center justify-between">
                <div className={`flex h-8 w-8 sm:h-9 sm:w-9 items-center justify-center rounded-lg ${stat.bg}`}>
                  {stat.icon}
                </div>
                <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 group-hover:bg-slate-200">
                  <span>View</span>
                  <span aria-hidden="true">&rarr;</span>
                </span>
              </div>

              <div className="mt-3">
                <span className="font-display text-2xl sm:text-3xl font-bold tracking-tight text-navy-900">
                  {stat.count}
                </span>
                <p className="mt-0.5 text-xs font-semibold text-slate-700 sm:text-sm">
                  {stat.label}
                </p>
                <p className="hidden sm:block text-[11px] text-slate-400">
                  {stat.sublabel}
                </p>
              </div>
            </button>
          )
        })}
      </div>
    </section>
  )
}
