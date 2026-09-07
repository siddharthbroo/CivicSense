import { useState, useMemo } from 'react'
import ComplaintCard from './ComplaintCard.jsx'

export default function ComplaintList({
  complaints = [],
  onNewComplaintClick,
  filter = 'ALL',
  onFilterChange,
  onRefresh,
  isLoading = false,
}) {
  const [internalFilter, setInternalFilter] = useState(filter)
  const [searchQuery, setSearchQuery] = useState('')

  const activeFilter = onFilterChange ? filter : internalFilter

  function handleTabClick(newFilter) {
    if (onFilterChange) {
      onFilterChange(newFilter)
    } else {
      setInternalFilter(newFilter)
    }
  }

  const filteredComplaints = useMemo(() => {
    return complaints.filter((c) => {
      // Status filter
      if (activeFilter === 'SUBMITTED' && c.status !== 'SUBMITTED') return false
      if (activeFilter === 'IN_PROGRESS' && c.status !== 'IN_PROGRESS') return false
      if (activeFilter === 'RESOLVED' && c.status !== 'RESOLVED' && c.status !== 'CLOSED') return false

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase()
        const matchDesc = c.description?.toLowerCase().includes(q)
        const matchAddr = c.address?.toLowerCase().includes(q)
        const matchId = String(c.id || '').toLowerCase().includes(q)
        return matchDesc || matchAddr || matchId
      }

      return true
    })
  }, [complaints, activeFilter, searchQuery])

  const counts = {
    ALL: complaints.length,
    SUBMITTED: complaints.filter((c) => c.status === 'SUBMITTED').length,
    IN_PROGRESS: complaints.filter((c) => c.status === 'IN_PROGRESS').length,
    RESOLVED: complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length,
  }

  return (
    <div className="space-y-5">
      {/* Controls Bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        {/* Filter Pills - Horizontal Scroll on Mobile */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
          {[
            { id: 'ALL', label: 'All Reports' },
            { id: 'SUBMITTED', label: 'Under Review' },
            { id: 'IN_PROGRESS', label: 'In Action' },
            { id: 'RESOLVED', label: 'Resolved' },
          ].map((tab) => {
            const isSelected = activeFilter === tab.id

            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => handleTabClick(tab.id)}
                className={`flex shrink-0 items-center gap-1.5 rounded-xl px-3.5 py-2 text-xs font-semibold transition-all duration-150 active:scale-95 ${
                  isSelected
                    ? 'bg-navy-900 text-white shadow-sm ring-1 ring-navy-950'
                    : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-navy-900'
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`rounded-full px-1.5 py-0.2 text-[10px] font-bold ${
                    isSelected ? 'bg-teal-500/25 text-teal-300' : 'bg-slate-100 text-slate-600'
                  }`}
                >
                  {counts[tab.id]}
                </span>
              </button>
            )
          })}
        </div>

        {/* Search Bar + Refresh */}
        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-64">
            <input
              type="text"
              placeholder="Search by keyword, street, or ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input-field py-2 pl-8 pr-7 text-xs rounded-xl"
            />
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            )}
          </div>

          {onRefresh && (
            <button
              type="button"
              onClick={onRefresh}
              disabled={isLoading}
              title="Refresh list"
              className="btn-secondary h-9 w-9 p-0 rounded-xl shrink-0"
            >
              ↻
            </button>
          )}
        </div>
      </div>

      {/* Complaint Cards */}
      {filteredComplaints.length > 0 ? (
        <div className="grid grid-cols-1 gap-4">
          {filteredComplaints.map((complaint) => (
            <ComplaintCard key={complaint.id || Math.random()} complaint={complaint} />
          ))}
        </div>
      ) : complaints.length === 0 ? (
        /* Zero State when no complaints exist at all */
        <div className="card flex flex-col items-center justify-center p-8 sm:p-12 text-center border-dashed border-2">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-teal-50 text-teal-700 ring-8 ring-teal-50/50">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
          </div>
          <h3 className="mt-4 font-display text-lg font-bold text-navy-900">
            No Complaints Filed Yet
          </h3>
          <p className="mt-1 max-w-sm text-xs text-slate-500 leading-relaxed">
            Spot a pothole, open garbage dump, broken street light, or sewage leak? Report it directly to your municipal ward officer.
          </p>
          <button
            type="button"
            onClick={onNewComplaintClick}
            className="btn-accent mt-5 px-5 py-2.5 text-xs font-bold rounded-xl shadow-sm active:scale-95"
          >
            + Report Your First Issue
          </button>
        </div>
      ) : (
        /* Empty Search Results */
        <div className="card p-8 text-center">
          <p className="text-sm font-semibold text-navy-900">No complaints matching "{searchQuery || activeFilter}"</p>
          <p className="mt-1 text-xs text-slate-400">Try adjusting your search terms or filter selection.</p>
          <button
            type="button"
            onClick={() => {
              handleTabClick('ALL')
              setSearchQuery('')
            }}
            className="btn-secondary mt-4 px-4 py-1.5 text-xs rounded-lg"
          >
            Reset All Filters
          </button>
        </div>
      )}
    </div>
  )
}
