import { useState } from 'react'
import {
  LocationPinIcon,
  UsersGroupIcon,
  ChatBubbleIcon,
  BookmarkIcon,
  ClockIcon,
  ThumbsUpIcon,
} from './HomeIcons.jsx'

export default function CommunityCard({
  complaint,
  onSupport,
  isSupporting = false,
  onCardClick,
}) {
  const [bookmarked, setBookmarked] = useState(false)

  const {
    imageUrl,
    title,
    description,
    address,
    category,
    priority,
    status,
    supportCount = 0,
    commentCount = 0,
    recurring = false,
    recurrenceCount = 0,
    supportedByCurrentUser = false,
    createdAt,
  } = complaint

  // Determine priority / status badge appearance strictly matching reference image
  function getBadge() {
    if (status === 'RESOLVED') {
      return {
        label: 'Resolved',
        className: 'bg-emerald-600 text-white',
      }
    }
    if (priority === 'HIGH' || priority === 'CRITICAL') {
      return {
        label: 'High Priority',
        className: 'bg-red-600 text-white',
      }
    }
    if (status === 'IN_PROGRESS') {
      return {
        label: 'In Progress',
        className: 'bg-amber-600 text-white',
      }
    }
    // Default / Under Review / Submitted
    return {
      label: 'Under Review',
      className: 'bg-amber-500 text-white',
    }
  }

  // Format relative timestamp matching "2 hours ago", "4 hours ago", "1 day ago"
  function formatRelativeTime(dateString) {
    if (!dateString) return 'Recently'
    try {
      const now = new Date()
      const past = new Date(dateString)
      const diffMs = now - past
      const diffHrs = Math.floor(diffMs / (1000 * 60 * 60))
      const diffDays = Math.floor(diffHrs / 24)

      if (diffHrs < 1) return 'Just now'
      if (diffHrs === 1) return '1 hour ago'
      if (diffHrs < 24) return `${diffHrs} hours ago`
      if (diffDays === 1) return '1 day ago'
      return `${diffDays} days ago`
    } catch {
      return 'Recently'
    }
  }

  const badge = getBadge()

  // Clean fallback category photo if no user image uploaded
  const fallbackImage =
    imageUrl ||
    (category?.toLowerCase().includes('garbage') || title?.toLowerCase().includes('garbage')
      ? 'https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?auto=format&fit=crop&w=600&q=80'
      : category?.toLowerCase().includes('light') || title?.toLowerCase().includes('light')
      ? 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80'
      : category?.toLowerCase().includes('footpath') || title?.toLowerCase().includes('footpath')
      ? 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=600&q=80'
      : category?.toLowerCase().includes('water') || title?.toLowerCase().includes('water')
      ? 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=600&q=80'
      : 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=600&q=80')

  return (
    <div
      onClick={() => onCardClick?.(complaint)}
      className="bg-white rounded-2xl border border-slate-150 shadow-xs hover:shadow-xl transition-all duration-300 overflow-hidden flex flex-col justify-between group h-full cursor-pointer"
    >
      {/* 1. Top Image Container */}
      <div className="relative h-48 w-full overflow-hidden bg-slate-100">
        <img
          src={fallbackImage}
          alt={title || 'Civic issue photo'}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
          onError={(e) => {
            e.target.onerror = null
            e.target.src = 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=600&q=80'
          }}
        />

        {/* Floating Priority/Status Badge (Top Left) */}
        <div className="absolute top-3 left-3 z-10">
          <span
            className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold tracking-wide shadow-sm backdrop-blur-md ${badge.className}`}
          >
            {badge.label}
          </span>
        </div>

        {/* Floating Bookmark Button (Top Right) */}
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation()
            setBookmarked(!bookmarked)
          }}
          aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark report'}
          title={bookmarked ? 'Bookmarked' : 'Bookmark report'}
          className="absolute top-3 right-3 z-10 p-1.5 rounded-lg bg-black/35 hover:bg-black/55 text-white backdrop-blur-md transition-all active:scale-90 cursor-pointer"
        >
          <BookmarkIcon
            className={`w-4 h-4 ${bookmarked ? 'text-amber-400 fill-amber-400' : 'text-white'}`}
            filled={bookmarked}
          />
        </button>
      </div>

      {/* 2. Card Content Area */}
      <div className="p-4 sm:p-5 flex-1 flex flex-col justify-between space-y-3.5">
        <div>
          {/* Title */}
          <h3 className="text-base font-bold text-slate-900 leading-snug group-hover:text-emerald-700 transition-colors line-clamp-1">
            {title || 'Civic Issue Reported'}
          </h3>

          {/* Location / Address with Green Pin */}
          <div className="flex items-center gap-1.5 mt-1.5 text-xs text-slate-600 font-medium">
            <LocationPinIcon className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="truncate">{address || 'Bengaluru, Karnataka'}</span>
          </div>

          {/* Short Description */}
          <p className="mt-2 text-xs text-slate-500 line-clamp-2 leading-relaxed">
            {description || 'Civic issue submitted by citizen. Tap to view corroboration and municipal status.'}
          </p>
        </div>

        {/* 3. Metric Engagement Rows */}
        <div className="pt-2 border-t border-slate-100 space-y-2 text-xs">
          {/* Row 1: Support count and Comment count */}
          <div className="flex items-center justify-between text-slate-600 font-medium">
            <div className="flex items-center gap-1.5">
              <UsersGroupIcon className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>
                {supportCount} {supportCount === 1 ? 'support' : 'supports'}
              </span>
            </div>

            <div className="flex items-center gap-1.5 text-slate-500">
              <ChatBubbleIcon className="w-4 h-4 text-slate-400 shrink-0" />
              <span>
                {commentCount} {commentCount === 1 ? 'comment' : 'comments'}
              </span>
            </div>
          </div>

          {/* Row 2: Relative timestamp and Recurrence Badge */}
          <div className="flex items-center justify-between text-slate-400 text-[11px]">
            <div className="flex items-center gap-1.5">
              <ClockIcon className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <span>{formatRelativeTime(createdAt)}</span>
            </div>

            {recurring && (
              <span className="inline-flex items-center gap-1 text-[10px] font-bold text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-md">
                Recurring ({recurrenceCount})
              </span>
            )}
          </div>
        </div>

        {/* 4. Full-Width "I'm affected too" Support Action Button */}
        <div className="pt-1">
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              onSupport(complaint)
            }}
            disabled={isSupporting}
            title={
              supportedByCurrentUser
                ? 'Click to remove your support'
                : 'Click to indicate you are affected too'
            }
            className={`w-full min-h-[42px] py-2.5 px-4 rounded-xl text-xs sm:text-sm font-bold flex items-center justify-center gap-2 transition-all active:scale-[0.97] cursor-pointer shadow-xs ${
              supportedByCurrentUser
                ? 'bg-emerald-700 hover:bg-emerald-800 text-white'
                : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200/60'
            }`}
          >
            {supportedByCurrentUser ? (
              <>
                <span className="w-4 h-4 rounded-full bg-white text-emerald-700 flex items-center justify-center text-[10px] font-extrabold">
                  ✓
                </span>
                <span>Supported</span>
              </>
            ) : (
              <>
                <ThumbsUpIcon className="w-4 h-4 text-emerald-700" />
                <span>I'm affected too</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
