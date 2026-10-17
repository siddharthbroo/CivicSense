import {
  DocumentIcon,
  CheckCircleIcon,
  ClockIcon,
  DotsHorizontalIcon,
} from '../home/HomeIcons.jsx'

export default function ReportSummary({
  complaints = [],
  activeFilter = 'ALL',
  onSelectFilter,
}) {
  const totalCount = complaints.length
  const resolvedCount = complaints.filter(
    (c) => c.status === 'RESOLVED' || c.status === 'CLOSED'
  ).length
  const inProgressCount = complaints.filter((c) => c.status === 'IN_PROGRESS').length
  const underReviewCount = complaints.filter((c) => c.status === 'SUBMITTED').length

  const items = [
    {
      id: 'ALL',
      title: 'Total Reports',
      count: totalCount,
      subtitle: "Issues you've reported",
      iconBg: 'bg-blue-50 text-blue-600 border-blue-100',
      icon: <DocumentIcon className="w-5 h-5 sm:w-6 sm:h-6" />,
      activeBorder: 'border-blue-400 ring-2 ring-blue-100',
    },
    {
      id: 'RESOLVED',
      title: 'Resolved',
      count: resolvedCount,
      subtitle: 'Completed successfully',
      iconBg: 'bg-emerald-50 text-emerald-600 border-emerald-100',
      icon: <CheckCircleIcon className="w-5 h-5 sm:w-6 sm:h-6" />,
      activeBorder: 'border-emerald-400 ring-2 ring-emerald-100',
    },
    {
      id: 'IN_PROGRESS',
      title: 'In Progress',
      count: inProgressCount,
      subtitle: 'Being addressed',
      iconBg: 'bg-amber-50 text-amber-600 border-amber-100',
      icon: <ClockIcon className="w-5 h-5 sm:w-6 sm:h-6" />,
      activeBorder: 'border-amber-400 ring-2 ring-amber-100',
    },
    {
      id: 'SUBMITTED',
      title: 'Under Review',
      count: underReviewCount,
      subtitle: 'Awaiting action',
      iconBg: 'bg-indigo-50 text-indigo-600 border-indigo-100',
      icon: <DotsHorizontalIcon className="w-5 h-5 sm:w-6 sm:h-6" />,
      activeBorder: 'border-indigo-400 ring-2 ring-indigo-100',
    },
  ]

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
      {items.map((item) => {
        const isSelected = activeFilter === item.id

        return (
          <div
            key={item.id}
            onClick={() => onSelectFilter?.(item.id)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                onSelectFilter?.(item.id)
              }
            }}
            className={`bg-white rounded-2xl p-3.5 sm:p-5 border transition-all duration-200 cursor-pointer text-left shadow-2xs hover:shadow-md hover:border-slate-300 flex flex-col justify-between ${
              isSelected
                ? item.activeBorder
                : 'border-slate-150'
            }`}
          >
            <div className="flex items-start justify-between gap-2">
              <div
                className={`p-2 sm:p-2.5 rounded-xl border ${item.iconBg} shrink-0 flex items-center justify-center`}
              >
                {item.icon}
              </div>
              <span className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight leading-none">
                {item.count}
              </span>
            </div>

            <div className="mt-3 sm:mt-4">
              <p className="text-xs sm:text-sm font-bold text-slate-800 leading-snug">
                {item.title}
              </p>
              <p className="text-[11px] sm:text-xs text-slate-400 mt-0.5 leading-tight truncate sm:whitespace-normal">
                {item.subtitle}
              </p>
            </div>
          </div>
        )
      })}
    </div>
  )
}

