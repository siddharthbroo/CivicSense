import { useNavigate } from 'react-router-dom'
import { DocumentIcon, SearchIcon } from '../home/HomeIcons.jsx'

export default function ReportEmptyState({
  isFiltered = false,
  onResetFilters,
}) {
  const navigate = useNavigate()

  if (isFiltered) {
    return (
      <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-8 sm:p-14 text-center shadow-2xs flex flex-col items-center justify-center">
        <div className="w-14 h-14 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mb-4">
          <SearchIcon className="w-6 h-6" />
        </div>
        <h3 className="text-base sm:text-lg font-bold text-slate-900">
          No matching reports found
        </h3>
        <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-md">
          We couldn't find any reports matching your active filter or search query.
        </p>
        <button
          type="button"
          onClick={onResetFilters}
          className="mt-5 px-5 py-2.5 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-all active:scale-95 cursor-pointer"
        >
          Reset Filters & Search
        </button>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-8 sm:p-14 text-center shadow-2xs flex flex-col items-center justify-center">
      <div className="w-16 h-16 rounded-3xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4 border border-emerald-100">
        <DocumentIcon className="w-8 h-8" />
      </div>
      <h3 className="text-lg sm:text-xl font-extrabold text-slate-900 tracking-tight">
        No reports yet
      </h3>
      <p className="text-xs sm:text-sm text-slate-500 mt-1.5 max-w-md leading-relaxed">
        Your civic hazard reports will appear here once you submit an issue. You'll be able to track municipal status, corroboration, and community discussion in real time.
      </p>
      <button
        type="button"
        onClick={() => navigate('/report-issue')}
        className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-xl font-bold text-xs sm:text-sm bg-emerald-700 hover:bg-emerald-800 text-white shadow-md transition-all active:scale-95 cursor-pointer"
      >
        <span>+ Report Your First Issue</span>
      </button>
    </div>
  )
}

