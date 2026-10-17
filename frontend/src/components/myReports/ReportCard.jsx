import {
  LocationPinIcon,
  CalendarIcon,
  DocumentIcon,
  ChevronRightIcon,
} from '../home/HomeIcons.jsx'

export default function ReportCard({ complaint, onViewDetails }) {
  const {
    id,
    title,
    description,
    category,
    address,
    latitude,
    longitude,
    status = 'SUBMITTED',
    priority,
    createdAt,
    imageUrl,
  } = complaint

  // Format full submission timestamp (e.g., "Aug 20, 2026 at 10:24 AM")
  function formatFullDate(dateString) {
    if (!dateString) return 'Recently submitted'
    try {
      const d = new Date(dateString)
      const datePart = d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      })
      const timePart = d.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      })
      return `${datePart} at ${timePart}`
    } catch {
      return dateString
    }
  }

  // Format short date for stepper (e.g., "Aug 20")
  function formatShortDate(dateString) {
    if (!dateString) return ''
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
      })
    } catch {
      return ''
    }
  }

  // Derive title from description if not directly provided
  const displayTitle =
    title && title.trim()
      ? title
      : description
      ? description.length > 55
        ? `${description.slice(0, 55)}...`
        : description
      : 'Civic Hazard Report'

  // Derive category label
  const displayCategory = category
    ? category.replace(/_/g, ' ')
    : description && description.toLowerCase().includes('pothole')
    ? 'Road & Pothole'
    : description && description.toLowerCase().includes('garbage')
    ? 'Garbage & Waste'
    : description && description.toLowerCase().includes('light')
    ? 'Street Light'
    : description && description.toLowerCase().includes('water')
    ? 'Water Supply'
    : 'Civic Report'

  // Determine priority badge appearance if priority is present from backend
  function getPriorityBadge() {
    if (!priority) return null
    const p = String(priority).toUpperCase()
    if (p === 'CRITICAL' || p === 'HIGH') {
      return {
        label: 'High Priority',
        className: 'bg-red-50 text-red-700 border-red-200/80',
      }
    }
    if (p === 'MEDIUM') {
      return {
        label: 'Medium Priority',
        className: 'bg-amber-50 text-amber-800 border-amber-200/80',
      }
    }
    return {
      label: 'Low Priority',
      className: 'bg-slate-100 text-slate-700 border-slate-200',
    }
  }

  const priorityBadge = getPriorityBadge()

  // Status badge appearance matching reference image
  function getStatusBadge() {
    if (status === 'RESOLVED') {
      return {
        label: 'Resolved',
        className: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      }
    }
    if (status === 'CLOSED') {
      return {
        label: 'Closed',
        className: 'bg-slate-100 text-slate-700 border-slate-200',
      }
    }
    if (status === 'IN_PROGRESS') {
      return {
        label: 'In Progress >',
        className: 'bg-blue-50 text-blue-700 border-blue-200',
      }
    }
    return {
      label: 'Under Review >',
      className: 'bg-amber-50 text-amber-800 border-amber-200',
    }
  }

  const statusBadge = getStatusBadge()

  // 4-Stage Lifecycle Stepper mapping strictly to CivicSense real statuses:
  // 1: Reported (SUBMITTED) -> 2: In Progress (IN_PROGRESS) -> 3: Resolved (RESOLVED) -> 4: Closed (CLOSED)
  function getStepIndex() {
    if (status === 'CLOSED') return 3
    if (status === 'RESOLVED') return 2
    if (status === 'IN_PROGRESS') return 1
    return 0
  }

  const currentStep = getStepIndex()

  const steps = [
    { label: 'Reported', date: formatShortDate(createdAt) },
    { label: 'In Progress', date: currentStep >= 1 ? 'Under Action' : '' },
    { label: 'Resolved', date: currentStep >= 2 ? 'Completed' : '' },
    { label: 'Closed', date: currentStep >= 3 ? 'Archived' : '' },
  ]

  // Fallback image if no evidence uploaded
  const displayImage =
    imageUrl ||
    'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=600&q=80'

  const locationText =
    address ||
    (latitude && longitude ? `${latitude.toFixed(4)}, ${longitude.toFixed(4)}` : 'Location verified')

  return (
    <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-4 sm:p-5 shadow-2xs hover:-translate-y-0.5 hover:shadow-md transition-all duration-200 flex flex-col lg:flex-row lg:items-center justify-between gap-5">
      {/* Left Block: Image + Core Details */}
      <div className="flex flex-col sm:flex-row items-start gap-4 flex-1 min-w-0">
        {/* Photo Thumbnail */}
        <div className="relative w-full sm:w-44 h-44 sm:h-28 rounded-xl sm:rounded-2xl overflow-hidden bg-slate-100 shrink-0 border border-slate-100">
          <img
            src={displayImage}
            alt={displayTitle}
            className="w-full h-full object-cover transition-transform duration-300 hover:scale-105"
            loading="lazy"
          />
        </div>

        {/* Text and Meta Info */}
        <div className="flex-1 min-w-0 space-y-2">
          {/* Title */}
          <h3 className="text-base sm:text-lg font-bold text-slate-900 tracking-tight leading-snug line-clamp-2">
            {displayTitle}
          </h3>

          {/* Category, Priority, and Reference ID Badges */}
          <div className="flex flex-wrap items-center gap-2 pt-0.5">
            {id && (
              <span className="font-mono text-[11px] font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                #{String(id).slice(0, 8).toUpperCase()}
              </span>
            )}

            <span className="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200/70">
              {displayCategory}
            </span>

            {priorityBadge && (
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-bold border ${priorityBadge.className}`}
              >
                {priorityBadge.label}
              </span>
            )}
          </div>

          {/* Location & Submission Date */}
          <div className="space-y-1 pt-1 text-xs text-slate-500 font-medium">
            <div className="flex items-center gap-1.5 truncate">
              <LocationPinIcon className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span className="truncate">{locationText}</span>
            </div>

            <div className="flex items-center gap-1.5">
              <CalendarIcon className="w-3.5 h-3.5 text-slate-400 shrink-0" />
              <span>{formatFullDate(createdAt)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Middle Block: Progress Stepper */}
      <div className="w-full lg:w-72 xl:w-80 px-2 sm:px-4 py-2 border-t lg:border-t-0 lg:border-l lg:border-r border-slate-100">
        <div className="flex items-center justify-between relative">
          {steps.map((step, idx) => {
            const isCompleted = idx <= currentStep
            const isCurrent = idx === currentStep

            return (
              <div key={step.label} className="flex flex-1 items-center last:flex-none">
                <div className="flex flex-col items-center text-center relative z-10">
                  {/* Step Dot */}
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-[11px] font-black transition-all ${
                      isCompleted
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : 'bg-white border-2 border-slate-200 text-slate-400'
                    }`}
                  >
                    {isCompleted ? '✓' : ''}
                  </div>

                  {/* Step Title */}
                  <span
                    className={`mt-1.5 text-[10px] sm:text-[11px] leading-tight font-bold ${
                      isCurrent
                        ? 'text-emerald-800'
                        : isCompleted
                        ? 'text-slate-800'
                        : 'text-slate-400 font-medium'
                    }`}
                  >
                    {step.label}
                  </span>

                  {/* Step Date/Subtitle if present */}
                  {step.date && (
                    <span className="text-[9px] text-slate-400 font-medium mt-0.5 hidden sm:block">
                      {step.date}
                    </span>
                  )}
                </div>

                {/* Connecting Line between steps */}
                {idx < steps.length - 1 && (
                  <div
                    className={`h-0.5 flex-1 mx-1.5 sm:mx-2 -mt-4 transition-colors ${
                      idx < currentStep ? 'bg-emerald-600' : 'bg-slate-200'
                    }`}
                  />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Right Block: Status Pill & View Details Action */}
      <div className="flex items-center lg:flex-col justify-between lg:justify-center gap-3 shrink-0 pt-2 lg:pt-0 border-t sm:border-t-0 border-slate-100">
        {/* Status Pill */}
        <span
          className={`inline-flex items-center px-3 py-1 rounded-xl text-xs font-extrabold border ${statusBadge.className}`}
        >
          {statusBadge.label}
        </span>

        {/* View Details Button */}
        <button
          type="button"
          onClick={() => onViewDetails?.(complaint)}
          className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 min-h-[44px] rounded-xl text-xs font-bold text-slate-700 hover:text-emerald-700 bg-slate-50 hover:bg-emerald-50/70 border border-slate-200 hover:border-emerald-200 transition-all active:scale-95 cursor-pointer shadow-2xs w-full sm:w-auto lg:w-32 touch-target"
        >
          <DocumentIcon className="w-3.5 h-3.5 text-slate-500" />
          <span>View Details</span>
          <ChevronRightIcon className="w-3.5 h-3.5 text-slate-400" />
        </button>
      </div>
    </div>
  )
}
