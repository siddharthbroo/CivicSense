import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getMyComplaints } from '../../api/complaintApi.js'
import { supportComplaint, removeSupport } from '../../api/communityApi.js'
import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'
import CommunityDetailModal from '../../components/home/CommunityDetailModal.jsx'
import ReportSummary from '../../components/myReports/ReportSummary.jsx'
import ReportFilters from '../../components/myReports/ReportFilters.jsx'
import ReportCard from '../../components/myReports/ReportCard.jsx'
import ReportSidebar from '../../components/myReports/ReportSidebar.jsx'
import ReportEmptyState from '../../components/myReports/ReportEmptyState.jsx'
import ReportLoadingSkeleton from '../../components/myReports/ReportLoadingSkeleton.jsx'
import Alert from '../../components/common/Alert.jsx'

export default function MyReportsPage() {
  const navigate = useNavigate()

  const [complaints, setComplaints] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  // Filter, search, and sort states
  const [activeFilter, setActiveFilter] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [sortOrder, setSortOrder] = useState('NEWEST')

  // Selected complaint for the detail modal
  const [selectedComplaintId, setSelectedComplaintId] = useState(null)
  const [isSupporting, setIsSupporting] = useState(false)

  // Fetch authenticated user's complaints from backend
  const loadComplaints = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const data = await getMyComplaints()
      setComplaints(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load user complaints:', err)
      setError(err.message || 'Unable to load your reports right now.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    let ignore = false
    async function fetchMyReports() {
      setIsLoading(true)
      setError('')
      try {
        const data = await getMyComplaints()
        if (!ignore) {
          setComplaints(Array.isArray(data) ? data : [])
        }
      } catch (err) {
        if (!ignore) {
          console.error('Failed to load user complaints:', err)
          setError(err.message || 'Unable to load your reports right now.')
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
        }
      }
    }

    fetchMyReports()
    return () => {
      ignore = true
    }
  }, [])

  // Calculate status counts for filter pills
  const counts = useMemo(() => {
    return {
      ALL: complaints.length,
      SUBMITTED: complaints.filter((c) => c.status === 'SUBMITTED').length,
      IN_PROGRESS: complaints.filter((c) => c.status === 'IN_PROGRESS').length,
      RESOLVED: complaints.filter(
        (c) => c.status === 'RESOLVED' || c.status === 'CLOSED'
      ).length,
      CLOSED: complaints.filter((c) => c.status === 'CLOSED').length,
    }
  }, [complaints])

  // Filter and sort complaints
  const filteredAndSortedComplaints = useMemo(() => {
    const list = complaints.filter((c) => {
      // 1. Status filter
      if (activeFilter === 'SUBMITTED' && c.status !== 'SUBMITTED') return false
      if (activeFilter === 'IN_PROGRESS' && c.status !== 'IN_PROGRESS') return false
      if (
        activeFilter === 'RESOLVED' &&
        c.status !== 'RESOLVED' &&
        c.status !== 'CLOSED'
      )
        return false
      if (activeFilter === 'CLOSED' && c.status !== 'CLOSED') return false

      // 2. Search query filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase()
        const matchTitle = (c.title || '').toLowerCase().includes(q)
        const matchDesc = (c.description || '').toLowerCase().includes(q)
        const matchCat = (c.category || '').toLowerCase().includes(q)
        const matchAddr = (c.address || '').toLowerCase().includes(q)
        const matchId = String(c.id || '').toLowerCase().includes(q)

        return matchTitle || matchDesc || matchCat || matchAddr || matchId
      }

      return true
    })

    // Sort order
    return list.sort((a, b) => {
      const dateA = new Date(a.createdAt || 0).getTime()
      const dateB = new Date(b.createdAt || 0).getTime()
      return sortOrder === 'OLDEST' ? dateA - dateB : dateB - dateA
    })
  }, [complaints, activeFilter, searchQuery, sortOrder])

  // Support toggle handler for the detail modal
  async function handleSupportToggle(complaint) {
    if (!complaint || !complaint.complaintId) return
    setIsSupporting(true)
    try {
      if (complaint.supportedByCurrentUser) {
        await removeSupport(complaint.complaintId)
      } else {
        await supportComplaint(complaint.complaintId)
      }
    } catch (err) {
      console.error('Support toggle failed in modal:', err)
    } finally {
      setIsSupporting(false)
    }
  }

  function handleResetFilters() {
    setActiveFilter('ALL')
    setSearchQuery('')
    setSortOrder('NEWEST')
  }

  return (
    <div className="min-h-screen bg-slate-25 flex flex-col font-sans text-slate-800 antialiased selection:bg-emerald-100 selection:text-emerald-900">
      {/* 1. Header Navigation */}
      <HomeNavbar />

      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-10 w-full">
        {/* Breadcrumb Navigation */}
        <nav className="flex items-center gap-2 text-xs font-semibold text-slate-500 mb-5">
          <Link
            to="/dashboard"
            className="hover:text-emerald-700 transition-colors"
          >
            Home
          </Link>
          <span className="text-slate-300">/</span>
          <span className="text-slate-900">My Reports</span>
        </nav>

        {/* Page Header matching reference image */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-slate-150/80 mb-6 sm:mb-8">
          <div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight leading-tight">
              My Reports
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-xl font-medium">
              Track the status of issues you've reported and see the impact you're creating in your neighborhood.
            </p>
          </div>

          <div className="flex items-center gap-3">
            {/* Cleaner Cities, Brighter Tomorrows subtle banner for larger screens */}
            <div className="hidden xl:flex flex-col text-right mr-3">
              <span className="text-xs font-extrabold text-emerald-800 leading-tight">
                Cleaner Cities
              </span>
              <span className="text-[11px] font-bold text-slate-400">
                Brighter Tomorrows
              </span>
            </div>

            {/* + Report a New Issue Button */}
            <button
              type="button"
              onClick={() => navigate('/report-issue')}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-xs sm:text-sm shadow-sm hover:shadow-md transition-all active:scale-95 cursor-pointer shrink-0"
            >
              <span className="text-base leading-none font-light">+</span>
              <span>Report a New Issue</span>
            </button>
          </div>
        </div>

        {/* Error State Banner */}
        {error && (
          <Alert variant="error" className="mb-6 flex items-center justify-between shadow-xs">
            <span className="text-xs sm:text-sm font-medium">{error}</span>
            <button
              type="button"
              onClick={loadComplaints}
              className="ml-4 font-bold text-xs underline hover:text-red-950 cursor-pointer"
            >
              Retry
            </button>
          </Alert>
        )}

        {/* Loading Skeleton */}
        {isLoading && complaints.length === 0 ? (
          <ReportLoadingSkeleton />
        ) : (
          <div className="space-y-6 sm:space-y-8">
            {/* 2. Summary Cards Overview (Calculated from real reports) */}
            <ReportSummary
              complaints={complaints}
              activeFilter={activeFilter}
              onSelectFilter={setActiveFilter}
            />

            {/* 3. Filter Pills + Search & Sort */}
            <ReportFilters
              activeFilter={activeFilter}
              onFilterChange={setActiveFilter}
              counts={counts}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              sortOrder={sortOrder}
              onSortChange={setSortOrder}
            />

            {/* 4. Main Two-Column Layout (Cards List on Left, Sidebar on Right) */}
            <div className="flex flex-col lg:flex-row items-start gap-6 sm:gap-8">
              {/* Reports List Area */}
              <div className="flex-1 w-full min-w-0 space-y-4">
                {filteredAndSortedComplaints.length > 0 ? (
                  filteredAndSortedComplaints.map((complaint) => (
                    <ReportCard
                      key={complaint.id}
                      complaint={complaint}
                      onViewDetails={() => setSelectedComplaintId(complaint.id)}
                    />
                  ))
                ) : (
                  <ReportEmptyState
                    isFiltered={complaints.length > 0}
                    onResetFilters={handleResetFilters}
                  />
                )}
              </div>

              {/* Sidebar on Desktop */}
              {complaints.length > 0 && (
                <ReportSidebar complaints={complaints} />
              )}
            </div>
          </div>
        )}
      </main>

      {/* 5. Detail Modal with full corroboration & discussion */}
      {selectedComplaintId && (
        <CommunityDetailModal
          complaintId={selectedComplaintId}
          onClose={() => setSelectedComplaintId(null)}
          onSupportToggle={handleSupportToggle}
          isSupporting={isSupporting}
        />
      )}

      {/* 6. Footer */}
      <HomeFooter />
    </div>
  )
}
