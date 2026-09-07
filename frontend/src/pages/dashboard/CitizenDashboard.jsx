import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../context/AuthContext.jsx'
import ComplaintStats from '../../components/complaint/ComplaintStats.jsx'
import ComplaintForm from '../../components/complaint/ComplaintForm.jsx'
import ComplaintList from '../../components/complaint/ComplaintList.jsx'
import CivicGuidelines from '../../components/complaint/CivicGuidelines.jsx'
import ComplaintSuccessModal from '../../components/complaint/ComplaintSuccessModal.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import Alert from '../../components/common/Alert.jsx'
import { getMyComplaints } from '../../api/complaintApi.js'

export default function CitizenDashboard() {
  const { user } = useAuth()

  const [complaints, setComplaints] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('form') // 'form' | 'list' | 'guidelines'
  const [activeListFilter, setActiveListFilter] = useState('ALL') // 'ALL' | 'SUBMITTED' | 'IN_PROGRESS' | 'RESOLVED'
  const [newlyCreatedComplaint, setNewlyCreatedComplaint] = useState(null)

  // Fetch real-time complaints directly from backend DB
  const loadComplaints = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const data = await getMyComplaints()
      setComplaints(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load complaints:', err)
      setError(err.message || 'Unable to fetch your complaints from the server.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    // Clean up legacy localStorage cache
    if (user?.mobileNumber) {
      localStorage.removeItem(`civicsense_complaints_${user.mobileNumber}`)
      localStorage.removeItem('civicsense_complaints_citizen')
    }
    loadComplaints()
  }, [loadComplaints, user?.mobileNumber])

  function handleComplaintCreated(newComplaint) {
    setComplaints((prev) => [newComplaint, ...prev])
    setNewlyCreatedComplaint(newComplaint)
    // Silently refresh to ensure server timestamp & state sync
    getMyComplaints()
      .then((data) => {
        if (Array.isArray(data)) setComplaints(data)
      })
      .catch(() => {})
  }

  function handleStatCardClick(filterId) {
    setActiveListFilter(filterId)
    setActiveTab('list')
  }

  return (
    <div className="space-y-6 sm:space-y-8 pb-12">
      {/* 1. Hero Civic Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-navy-950 via-navy-900 to-navy-800 p-5 sm:p-7 text-white shadow-elevated border border-navy-700/50">
        {/* Subtle Decorative Background Circles */}
        <div className="pointer-events-none absolute -right-12 -top-12 h-64 w-64 rounded-full bg-teal-500/10 blur-3xl" aria-hidden="true" />
        <div className="pointer-events-none absolute -left-12 -bottom-12 h-48 w-48 rounded-full bg-navy-500/20 blur-2xl" aria-hidden="true" />

        <div className="relative z-10 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-teal-500/20 px-2.5 py-0.5 text-xs font-semibold text-teal-300 ring-1 ring-inset ring-teal-400/40 backdrop-blur-sm">
                <span className="h-1.5 w-1.5 rounded-full bg-teal-400 animate-pulse" />
                Verified Citizen Account
              </span>
              <span className="text-xs text-slate-400">
                · {user?.mobileNumber ? `+91 ${user.mobileNumber}` : 'Portal Active'}
              </span>
            </div>

            <h1 className="mt-2 font-display text-xl sm:text-2xl lg:text-3xl font-bold tracking-tight text-white">
              Namaste, {user?.name || 'Citizen'}
            </h1>
            <p className="mt-1 max-w-xl text-xs sm:text-sm text-slate-300 leading-relaxed">
              Report civic hazards in your ward, track real-time resolution by municipal engineers, and keep our neighborhood clean and safe.
            </p>
          </div>

          {/* Quick Header CTAs */}
          <div className="flex shrink-0 items-center gap-2 pt-1 sm:pt-0">
            <button
              type="button"
              onClick={() => setActiveTab('form')}
              className={`btn-accent text-xs font-bold px-4 py-2 rounded-xl shadow-sm transition-all active:scale-95 ${
                activeTab === 'form' ? 'ring-2 ring-white/30' : ''
              }`}
            >
              + Report Issue
            </button>
            <button
              type="button"
              onClick={() => {
                setActiveListFilter('ALL')
                setActiveTab('list')
              }}
              className="btn-secondary border-navy-700 bg-navy-800/80 text-xs text-slate-200 hover:bg-navy-700 px-3.5 py-2 rounded-xl"
            >
              My Reports ({complaints.length})
            </button>
          </div>
        </div>
      </div>

      {/* 2. Error Notification Banner */}
      {error && (
        <Alert variant="error" className="flex items-center justify-between shadow-sm">
          <span className="text-xs sm:text-sm">{error}</span>
          <button
            type="button"
            onClick={loadComplaints}
            className="ml-4 font-bold text-xs underline hover:text-red-950"
          >
            Retry
          </button>
        </Alert>
      )}

      {/* 3. Summary Metric Cards */}
      <ComplaintStats
        complaints={complaints}
        onTabChange={setActiveTab}
        activeFilter={activeListFilter}
        onFilterChange={handleStatCardClick}
      />

      {/* 4. Modern Segmented Tab Controls */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-2">
        <nav className="flex space-x-1.5 sm:space-x-2 bg-slate-100/90 p-1 rounded-xl border border-slate-200 shadow-sm" aria-label="Dashboard views">
          <button
            type="button"
            onClick={() => setActiveTab('form')}
            className={`flex items-center gap-1.5 rounded-lg px-3.5 sm:px-4 py-2 text-xs font-bold transition-all duration-150 ${
              activeTab === 'form'
                ? 'bg-white text-navy-950 shadow-sm ring-1 ring-slate-200/80'
                : 'text-slate-600 hover:text-navy-900'
            }`}
          >
            <span>✍️</span>
            <span>File Complaint</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('list')}
            className={`flex items-center gap-1.5 rounded-lg px-3.5 sm:px-4 py-2 text-xs font-bold transition-all duration-150 ${
              activeTab === 'list'
                ? 'bg-white text-navy-950 shadow-sm ring-1 ring-slate-200/80'
                : 'text-slate-600 hover:text-navy-900'
            }`}
          >
            <span>📋</span>
            <span>My Reports</span>
            <span
              className={`rounded-full px-1.5 py-0.2 text-[10px] font-bold ${
                activeTab === 'list'
                  ? 'bg-teal-100 text-teal-800'
                  : 'bg-slate-200 text-slate-700'
              }`}
            >
              {complaints.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('guidelines')}
            className={`flex items-center gap-1.5 rounded-lg px-3.5 sm:px-4 py-2 text-xs font-bold transition-all duration-150 ${
              activeTab === 'guidelines'
                ? 'bg-white text-navy-950 shadow-sm ring-1 ring-slate-200/80'
                : 'text-slate-600 hover:text-navy-900'
            }`}
          >
            <span>💡</span>
            <span className="hidden sm:inline">Civic Guidelines & Helplines</span>
            <span className="sm:hidden">Help</span>
          </button>
        </nav>

        {/* Global Refresh Indicator */}
        <button
          type="button"
          onClick={loadComplaints}
          disabled={isLoading}
          title="Refresh complaint data"
          className="hidden sm:inline-flex items-center gap-1 text-xs font-semibold text-slate-500 hover:text-teal-700 disabled:opacity-50"
        >
          {isLoading ? (
            <>
              <Spinner size={13} />
              <span>Updating…</span>
            </>
          ) : (
            <>
              <span>↻ Sync Data</span>
            </>
          )}
        </button>
      </div>

      {/* 5. Tab Content Views */}
      <div>
        {activeTab === 'form' && (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <ComplaintForm onComplaintCreated={handleComplaintCreated} />
            </div>
            <div className="space-y-6">
              <ReportingTipsCard onLearnMore={() => setActiveTab('guidelines')} />
            </div>
          </div>
        )}

        {activeTab === 'list' && (
          isLoading && complaints.length === 0 ? (
            <div className="card flex flex-col items-center justify-center p-12 text-slate-500 border border-slate-200 shadow-sm">
              <Spinner size={28} className="text-teal-600 mb-3" />
              <p className="text-sm font-semibold text-navy-900">Loading your complaint history...</p>
              <p className="text-xs text-slate-400 mt-1">Connecting to municipal database</p>
            </div>
          ) : (
            <ComplaintList
              complaints={complaints}
              filter={activeListFilter}
              onFilterChange={setActiveListFilter}
              onNewComplaintClick={() => setActiveTab('form')}
              onRefresh={loadComplaints}
              isLoading={isLoading}
            />
          )
        )}

        {activeTab === 'guidelines' && <CivicGuidelines />}
      </div>

      {/* 6. Submission Confirmation Dialog */}
      {newlyCreatedComplaint && (
        <ComplaintSuccessModal
          complaint={newlyCreatedComplaint}
          onClose={() => setNewlyCreatedComplaint(null)}
          onViewList={() => {
            setNewlyCreatedComplaint(null)
            setActiveTab('list')
          }}
        />
      )}
    </div>
  )
}

/**
 * Side guidance widget shown on the complaint creation screen.
 */
function ReportingTipsCard({ onLearnMore }) {
  return (
    <div className="card p-5 sm:p-6 border border-slate-200 space-y-4">
      <div className="flex items-center gap-2">
        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-teal-100 text-teal-800 text-xs">
          ⚡
        </span>
        <h3 className="font-display text-sm font-bold text-navy-900">
          Fast-Track Resolution Tips
        </h3>
      </div>

      <ul className="space-y-3 text-xs leading-relaxed text-slate-600">
        <li className="flex items-start gap-2">
          <span className="text-teal-700 font-bold">•</span>
          <span>
            <strong className="text-navy-950 font-semibold">Enable GPS:</strong> Precise coordinates guide municipal ground vehicles without delays.
          </span>
        </li>
        <li className="flex items-start gap-2">
          <span className="text-teal-700 font-bold">•</span>
          <span>
            <strong className="text-navy-950 font-semibold">Clear Photo Proof:</strong> Daylight captures showing surrounding landmarks are resolved 40% faster.
          </span>
        </li>
        <li className="flex items-start gap-2">
          <span className="text-teal-700 font-bold">•</span>
          <span>
            <strong className="text-navy-950 font-semibold">Nearby Landmark:</strong> Mention shop names, school gates, or pillar numbers.
          </span>
        </li>
      </ul>

      <div className="rounded-xl bg-teal-50/70 p-3.5 border border-teal-200/70 text-[11px] text-teal-950 leading-relaxed">
        <strong>Government Direct Link:</strong> Complaints filed via CivicSense are automatically entered into the central municipal grievance registry.
      </div>

      <button
        type="button"
        onClick={onLearnMore}
        className="inline-flex items-center gap-1 text-xs font-bold text-teal-800 hover:text-teal-950 hover:underline"
      >
        <span>View emergency helplines & rules</span>
        <span aria-hidden="true">&rarr;</span>
      </button>
    </div>
  )
}
