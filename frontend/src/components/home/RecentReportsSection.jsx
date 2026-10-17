import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import CommunityCard from './CommunityCard.jsx'
import CommunityDetailModal from './CommunityDetailModal.jsx'
import {
  ArrowRightIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  MegaphoneIcon,
  RoadIcon,
  TrashIcon,
  LightBulbIcon,
  WaterDropIcon,
  DrainageIcon,
  PlantIcon,
  DotsHorizontalIcon,
} from './HomeIcons.jsx'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function RecentReportsSection({
  complaints = [],
  onSupportToggle,
  supportingId = null,
  isLoading = false,
  error = '',
  onRetry,
  onCommentCountChange,
  isNearbyOnly = false,
  onToggleNearby,
}) {
  const [selectedFilter, setSelectedFilter] = useState('ALL')
  const [detailComplaintId, setDetailComplaintId] = useState(null)
  const scrollContainerRef = useRef(null)
  const sectionRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      gsap.from('.reports-header', {
        opacity: 0,
        y: 18,
        duration: 0.6,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 85%',
          once: true,
        },
      })

      gsap.from('.category-tabs', {
        opacity: 0,
        y: 12,
        duration: 0.5,
        delay: 0.15,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 85%',
          once: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  // Category filters strictly matching reference image pills and icons
  const CATEGORY_TABS = [
    { id: 'ALL', label: 'All', icon: null },
    { id: 'ROAD', label: 'Road & Pothole', icon: RoadIcon },
    { id: 'GARBAGE', label: 'Garbage & Waste', icon: TrashIcon },
    { id: 'LIGHT', label: 'Street Light', icon: LightBulbIcon },
    { id: 'WATER', label: 'Water Supply', icon: WaterDropIcon },
    { id: 'SEWAGE', label: 'Sewage & Drainage', icon: DrainageIcon },
    { id: 'GREENERY', label: 'Parks & Greenery', icon: PlantIcon },
    { id: 'OTHERS', label: 'Others', icon: DotsHorizontalIcon },
  ]

  // Filter complaints client-side based on the selected tab
  const filteredComplaints = complaints.filter((c) => {
    if (selectedFilter === 'ALL') return true

    const cat = (c.category || '').toUpperCase()
    const text = `${c.title || ''} ${c.description || ''}`.toLowerCase()

    if (selectedFilter === 'ROAD') {
      return (
        cat.includes('ROAD') ||
        cat.includes('POTHOLE') ||
        cat.includes('FOOTPATH') ||
        cat.includes('INFRASTRUCTURE') ||
        text.includes('pothole') ||
        text.includes('road') ||
        text.includes('footpath')
      )
    }

    if (selectedFilter === 'GARBAGE') {
      return (
        cat.includes('GARBAGE') ||
        cat.includes('WASTE') ||
        cat.includes('SANITATION') ||
        text.includes('garbage') ||
        text.includes('waste') ||
        text.includes('trash') ||
        text.includes('bin')
      )
    }

    if (selectedFilter === 'LIGHT') {
      return (
        cat.includes('LIGHT') ||
        cat.includes('ELECTRIC') ||
        text.includes('street light') ||
        text.includes('light') ||
        text.includes('lamp')
      )
    }

    if (selectedFilter === 'WATER') {
      return (
        cat.includes('WATER') ||
        text.includes('water') ||
        text.includes('pipeline') ||
        text.includes('leak')
      )
    }

    if (selectedFilter === 'SEWAGE') {
      return (
        cat.includes('SEWAGE') ||
        cat.includes('DRAIN') ||
        text.includes('sewage') ||
        text.includes('drain') ||
        text.includes('gutter')
      )
    }

    if (selectedFilter === 'GREENERY') {
      return (
        cat.includes('PARK') ||
        cat.includes('TREE') ||
        cat.includes('GREEN') ||
        text.includes('park') ||
        text.includes('tree') ||
        text.includes('garden')
      )
    }

    if (selectedFilter === 'OTHERS') {
      const isRoad =
        cat.includes('ROAD') ||
        cat.includes('POTHOLE') ||
        text.includes('road') ||
        text.includes('pothole')
      const isGarbage =
        cat.includes('GARBAGE') || cat.includes('WASTE') || text.includes('garbage')
      const isLight =
        cat.includes('LIGHT') || cat.includes('ELECTRIC') || text.includes('light')
      const isWater = cat.includes('WATER') || text.includes('water')
      const isSewage =
        cat.includes('SEWAGE') || cat.includes('DRAIN') || text.includes('drain')
      const isGreen = cat.includes('PARK') || text.includes('park')
      return !isRoad && !isGarbage && !isLight && !isWater && !isSewage && !isGreen
    }

    return true
  })

  // Horizontal scroll controls
  function scroll(direction) {
    if (scrollContainerRef.current) {
      const scrollAmount = 340
      scrollContainerRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth',
      })
    }
  }

  function handleCardClick(complaint) {
    if (complaint && complaint.complaintId) {
      setDetailComplaintId(complaint.complaintId)
    }
  }

  return (
    <section
      ref={sectionRef}
      id="community-reports"
      className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-20"
    >
      {/* Section Header Matching Reference */}
      <div className="reports-header flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6 sm:mb-7">
        <div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Recent Community Reports
          </h2>
          <p className="text-sm sm:text-base text-slate-500 mt-1 font-normal">
            Real issues. Real people. Real change.
          </p>
        </div>

        {/* Header Actions */}
        <div className="flex flex-wrap items-center gap-3">
          {onToggleNearby && (
            <button
              type="button"
              onClick={onToggleNearby}
              className={`inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer border active:scale-95 touch-target ${
                isNearbyOnly
                  ? 'bg-emerald-700 text-white border-emerald-700 shadow-sm'
                  : 'bg-white text-slate-700 border-slate-200 hover:border-emerald-500 hover:text-emerald-700'
              }`}
            >
              <span>📍</span>
              <span>{isNearbyOnly ? 'Nearby Only (5km)' : 'Near Me'}</span>
            </button>
          )}

          <button
            type="button"
            onClick={() => setSelectedFilter('ALL')}
            className="inline-flex items-center gap-1.5 text-sm font-bold text-emerald-700 hover:text-emerald-800 transition-colors cursor-pointer group py-2"
          >
            <span>View All Reports</span>
            <ArrowRightIcon className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
          </button>
        </div>
      </div>

      {/* Category Filter Pills Matching Reference Design */}
      <div className="category-tabs flex items-center gap-2 overflow-x-auto pb-3 mb-6 no-scrollbar -mx-4 px-4 sm:mx-0 sm:px-0">
        {CATEGORY_TABS.map((tab) => {
          const Icon = tab.icon
          const isActive = selectedFilter === tab.id

          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setSelectedFilter(tab.id)}
              className={`flex items-center gap-2 px-3.5 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer whitespace-nowrap active:scale-95 touch-target ${
                isActive
                  ? 'bg-emerald-800 text-white shadow-sm'
                  : 'bg-slate-100/80 text-slate-700 hover:bg-slate-200/90'
              }`}
            >
              {Icon && <Icon className="w-4 h-4 shrink-0" />}
              <span>{tab.label}</span>
            </button>
          )
        })}
      </div>

      {/* API Error Notification */}
      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center justify-between">
          <span>{error}</span>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="font-bold underline text-xs hover:text-red-900 cursor-pointer"
            >
              Retry
            </button>
          )}
        </div>
      )}

      {/* Content Rendering: Loading / Empty / Cards Carousel */}
      {isLoading && complaints.length === 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="bg-white rounded-2xl border border-slate-150 p-4 animate-pulse space-y-4 shadow-xs"
            >
              <div className="h-48 bg-slate-200 rounded-xl w-full" />
              <div className="h-4 bg-slate-200 rounded w-3/4" />
              <div className="h-3 bg-slate-200 rounded w-1/2" />
              <div className="h-9 bg-slate-200 rounded-xl w-full" />
            </div>
          ))}
        </div>
      ) : complaints.length === 0 ? (
        /* Empty State */
        <div className="rounded-3xl border-2 border-dashed border-slate-200 bg-white p-8 sm:p-12 text-center shadow-xs">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700 mb-4 shadow-xs">
            <MegaphoneIcon className="h-8 w-8" />
          </div>
          <h3 className="text-lg sm:text-xl font-bold text-slate-900">
            No community reports yet
          </h3>
          <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
            Be the first to report an issue. Your report will appear here for neighboring citizens to corroborate.
          </p>
          <button
            type="button"
            onClick={() => navigate('/report-issue')}
            className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-sm shadow-sm transition-all active:scale-95 cursor-pointer touch-target"
          >
            <span>+ Report an Issue</span>
          </button>
        </div>
      ) : filteredComplaints.length === 0 ? (
        /* Filter Zero State */
        <div className="rounded-3xl border border-slate-200 bg-white p-8 sm:p-10 text-center">
          <p className="text-base font-semibold text-slate-700">
            No reports found for this category.
          </p>
          <button
            type="button"
            onClick={() => setSelectedFilter('ALL')}
            className="mt-3 text-sm font-bold text-emerald-700 hover:underline cursor-pointer"
          >
            View all reports
          </button>
        </div>
      ) : (
        /* Cards Carousel / Grid */
        <div className="relative group">
          {/* Previous Carousel Button */}
          <button
            type="button"
            onClick={() => scroll('left')}
            aria-label="Previous reports"
            className="hidden md:flex absolute -left-5 top-1/2 -translate-y-1/2 z-20 w-11 h-11 rounded-full bg-white shadow-xl border border-slate-200 text-slate-700 hover:text-emerald-700 hover:scale-105 active:scale-95 items-center justify-center transition-all cursor-pointer"
          >
            <ChevronLeftIcon className="w-5 h-5" />
          </button>

          {/* Next Carousel Button (Prominent in reference image) */}
          <button
            type="button"
            onClick={() => scroll('right')}
            aria-label="Next reports"
            className="hidden md:flex absolute -right-5 top-1/2 -translate-y-1/2 z-20 w-11 h-11 rounded-full bg-white shadow-xl border border-slate-200 text-slate-700 hover:text-emerald-700 hover:scale-105 active:scale-95 items-center justify-center transition-all cursor-pointer"
          >
            <ChevronRightIcon className="w-5 h-5" />
          </button>

          {/* Scrollable Responsive Track */}
          <div
            ref={scrollContainerRef}
            className="flex overflow-x-auto snap-x snap-mandatory scroll-smooth gap-4 sm:gap-6 pb-4 no-scrollbar -mx-4 px-4 sm:mx-0 sm:px-0"
          >
            {filteredComplaints.map((complaint) => (
              <div
                key={complaint.complaintId}
                className="w-[84vw] sm:w-[320px] lg:w-[calc(25%-18px)] shrink-0 snap-start flex-none flex"
              >
                <div className="w-full flex">
                  <CommunityCard
                    complaint={complaint}
                    onSupport={onSupportToggle}
                    isSupporting={supportingId === complaint.complaintId}
                    onCardClick={handleCardClick}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Community Complaint Detail Modal */}
      {detailComplaintId && (
        <CommunityDetailModal
          complaintId={detailComplaintId}
          onClose={() => setDetailComplaintId(null)}
          onSupportToggle={onSupportToggle}
          isSupporting={supportingId === detailComplaintId}
          onCommentCountChange={onCommentCountChange}
        />
      )}
    </section>
  )
}
