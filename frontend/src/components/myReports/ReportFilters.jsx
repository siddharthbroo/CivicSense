import { SearchIcon } from '../home/HomeIcons.jsx'

export default function ReportFilters({
  activeFilter = 'ALL',
  onFilterChange,
  counts = {},
  searchQuery = '',
  onSearchChange,
  sortOrder = 'NEWEST',
  onSortChange,
}) {
  const filterTabs = [
    { id: 'ALL', label: 'All', count: counts.ALL ?? 0 },
    { id: 'SUBMITTED', label: 'Under Review', count: counts.SUBMITTED ?? 0 },
    { id: 'IN_PROGRESS', label: 'In Progress', count: counts.IN_PROGRESS ?? 0 },
    { id: 'RESOLVED', label: 'Resolved', count: counts.RESOLVED ?? 0 },
  ]

  // Add Closed tab if closed complaints exist
  if (counts.CLOSED && counts.CLOSED > 0) {
    filterTabs.push({ id: 'CLOSED', label: 'Closed', count: counts.CLOSED })
  }

  return (
    <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
      {/* Horizontally Scrollable Filter Pills on Mobile */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0 no-scrollbar snap-x touch-pan-x -mx-4 px-4 sm:mx-0 sm:px-0">
        {filterTabs.map((tab) => {
          const isSelected = activeFilter === tab.id

          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => onFilterChange(tab.id)}
              className={`shrink-0 inline-flex items-center gap-1.5 px-3.5 sm:px-4 py-2 rounded-xl text-xs font-bold transition-all duration-150 active:scale-95 cursor-pointer select-none ${
                isSelected
                  ? 'bg-emerald-800 text-white shadow-xs'
                  : 'bg-white border border-slate-200 text-slate-700 hover:border-slate-300 hover:bg-slate-50'
              }`}
            >
              <span>{tab.label}</span>
              <span
                className={`px-1.5 py-0.5 rounded-md text-[10px] font-extrabold leading-none ${
                  isSelected
                    ? 'bg-emerald-700 text-emerald-100'
                    : 'bg-slate-100 text-slate-600'
                }`}
              >
                {tab.count}
              </span>
            </button>
          )
        })}
      </div>

      {/* Search Bar and Sort Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 w-full lg:w-auto">
        {/* Search Input */}
        <div className="relative flex-1 lg:w-64">
          <SearchIcon className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="Search your reports..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full pl-9 pr-8 py-2.5 text-xs sm:text-sm bg-white border border-slate-200 rounded-xl focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 focus:outline-none transition-all placeholder:text-slate-400 shadow-2xs"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => onSearchChange('')}
              aria-label="Clear search"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 p-1 rounded-md cursor-pointer"
            >
              ✕
            </button>
          )}
        </div>

        {/* Sort Dropdown */}
        <div className="relative w-full sm:w-auto shrink-0">
          <select
            value={sortOrder}
            onChange={(e) => onSortChange(e.target.value)}
            aria-label="Sort reports"
            className="w-full sm:w-auto appearance-none bg-white border border-slate-200 text-slate-700 font-bold text-xs py-2.5 pl-3 pr-8 rounded-xl focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 focus:outline-none cursor-pointer shadow-2xs"
          >
            <option value="NEWEST">Newest First</option>
            <option value="OLDEST">Oldest First</option>
          </select>
          <div className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M6 9l6 6 6-6" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  )
}
