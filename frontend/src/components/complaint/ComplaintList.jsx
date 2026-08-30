import { useState, useMemo } from 'react'
import ComplaintCard from './ComplaintCard.jsx'

export default function ComplaintList({ complaints = [], onNewComplaintClick }) {
  const [filter, setFilter] = useState('ALL') // ALL | SUBMITTED | IN_PROGRESS | RESOLVED
  const [searchQuery, setSearchQuery] = useState('')

  const filteredComplaints = useMemo(() => {
    return complaints.filter((c) => {
      // Status filter
      if (filter === 'SUBMITTED' && c.status !== 'SUBMITTED') return false
      if (filter === 'IN_PROGRESS' && c.status !== 'IN_PROGRESS') return false
      if (filter === 'RESOLVED' && c.status !== 'RESOLVED' && c.status !== 'CLOSED') return false

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
  }, [complaints, filter, searchQuery])

  const counts = {
    ALL: complaints.length,
    SUBMITTED: complaints.filter((c) => c.status === 'SUBMITTED').length,
    IN_PROGRESS: complaints.filter((c) => c.status === 'IN_PROGRESS').length,
    RESOLVED: complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length,
  }

  return (
    <div className="space-y-6">
      {/* Search and Filter Bar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        {/* Filter Pills */}
        <div className="flex flex-wrap items-center gap-1.5 rounded-lg border border-slate-200 bg-white p-1 shadow-sm">
          {[
            { id: 'ALL', label: 'All' },
            { id: 'SUBMITTED', label: 'Submitted' },
            { id: 'IN_PROGRESS', label: 'In Progress' },
            { id: 'RESOLVED', label: 'Resolved' },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setFilter(tab.id)}
              className={`flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-semibold transition-all ${
                filter === tab.id
                  ? 'bg-navy-800 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-navy-900'
              }`}
            >
              <span>{tab.label}</span>
              <span
                className={`rounded-full px-1.5 py-0.2 text-[10px] ${
                  filter === tab.id ? 'bg-navy-900 text-teal-300' : 'bg-slate-100 text-slate-600'
                }`}
              >
                {counts[tab.id]}
              </span>
            </button>
          ))}
        </div>

        {/* Search Box */}
        <div className="relative flex-1 sm:max-w-xs">
          <input
            type="text"
            placeholder="Search complaints..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input-field py-1.5 pl-8 text-xs placeholder:text-slate-400"
          />
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400"
            aria-hidden="true"
          >
            <path
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600"
            >
              &times;
            </button>
          )}
        </div>
      </div>

      {/* Complaints List Cards */}
      {filteredComplaints.length > 0 ? (
        <div className="grid grid-cols-1 gap-4">
          {filteredComplaints.map((complaint) => (
            <ComplaintCard key={complaint.id || Math.random()} complaint={complaint} />
          ))}
        </div>
      ) : complaints.length === 0 ? (
        /* Zero state when no complaints exist at all */
        <div className="card flex flex-col items-center justify-center p-10 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-teal-50 text-teal-700">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <h3 className="mt-4 text-base font-bold text-navy-900">No Complaints Reported Yet</h3>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            You haven't submitted any civic complaints yet. Notice an issue in your locality? Report it now to notify municipal authorities.
          </p>
          <button
            type="button"
            onClick={onNewComplaintClick}
            className="btn-accent mt-5 text-xs font-semibold"
          >
            + File a Civic Complaint
          </button>
        </div>
      ) : (
        /* Empty search results */
        <div className="card p-8 text-center">
          <p className="text-sm font-medium text-slate-600">No complaints matching your criteria.</p>
          <p className="mt-1 text-xs text-slate-400">Try adjusting your search query or status filter.</p>
          <button
            type="button"
            onClick={() => {
              setFilter('ALL')
              setSearchQuery('')
            }}
            className="btn-secondary mt-4 px-3 py-1.5 text-xs"
          >
            Reset Filters
          </button>
        </div>
      )}
    </div>
  )
}

