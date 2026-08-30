import { useState } from 'react'
import StatusBadge from '../common/StatusBadge.jsx'

export default function ComplaintCard({ complaint }) {
  const [showImageModal, setShowImageModal] = useState(false)

  const {
    id,
    description,
    latitude,
    longitude,
    address,
    status = 'SUBMITTED',
    createdAt,
    imageUrl,
  } = complaint

  const formattedDate = createdAt
    ? new Date(createdAt).toLocaleString('en-IN', {
        dateStyle: 'medium',
        timeStyle: 'short',
      })
    : 'Recently'

  const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`

  // Step tracker calculation
  const steps = [
    { key: 'SUBMITTED', label: 'Submitted' },
    { key: 'IN_PROGRESS', label: 'In Action' },
    { key: 'RESOLVED', label: 'Resolved' },
  ]

  function getStepIndex(currentStatus) {
    if (currentStatus === 'RESOLVED' || currentStatus === 'CLOSED') return 2
    if (currentStatus === 'IN_PROGRESS') return 1
    return 0
  }

  const currentStepIdx = getStepIndex(status)

  return (
    <>
      <div className="card transition-all duration-150 hover:border-slate-300 hover:shadow-elevated">
        <div className="p-5 sm:p-6">
          {/* Header row */}
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-semibold text-slate-500">
                REF #{id ? String(id).slice(0, 8).toUpperCase() : 'NEW'}
              </span>
              <span className="text-slate-300">·</span>
              <span className="text-xs text-slate-500">{formattedDate}</span>
            </div>
            <StatusBadge status={status} />
          </div>

          {/* Body content */}
          <div className="mt-4 flex flex-col-reverse gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex-1 space-y-3">
              <p className="text-sm leading-relaxed text-slate-800 whitespace-pre-wrap">
                {description}
              </p>

              {/* Address / Location details */}
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-600">
                {address && (
                  <div className="flex items-center gap-1">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-slate-400" aria-hidden="true">
                      <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    <span className="font-medium text-slate-700">{address}</span>
                  </div>
                )}

                <div className="flex items-center gap-1">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-slate-400" aria-hidden="true">
                    <path d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <span className="font-mono text-slate-500">
                    {latitude?.toFixed(4)}, {longitude?.toFixed(4)}
                  </span>
                  <a
                    href={mapsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="ml-1 font-medium text-teal-700 hover:text-teal-900 hover:underline"
                  >
                    (View Map &rarr;)
                  </a>
                </div>
              </div>
            </div>

            {/* Photo preview thumbnail */}
            {imageUrl && (
              <div className="relative shrink-0">
                <button
                  type="button"
                  onClick={() => setShowImageModal(true)}
                  className="group relative block overflow-hidden rounded-lg border border-slate-200"
                  title="Click to zoom image"
                >
                  <img
                    src={imageUrl}
                    alt="Issue preview"
                    className="h-24 w-24 object-cover transition-transform duration-200 group-hover:scale-105 sm:h-28 sm:w-28"
                  />
                  <div className="absolute inset-0 flex items-center justify-center bg-navy-900/40 opacity-0 transition-opacity group-hover:opacity-100">
                    <span className="rounded bg-navy-900/80 px-2 py-0.5 text-[10px] font-semibold text-white">
                      Zoom
                    </span>
                  </div>
                </button>
              </div>
            )}
          </div>

          {/* Progress timeline bar */}
          <div className="mt-5 border-t border-slate-100 pt-4">
            <div className="flex items-center justify-between">
              {steps.map((step, idx) => {
                const isCompleted = idx <= currentStepIdx
                const isCurrent = idx === currentStepIdx

                return (
                  <div key={step.key} className="flex flex-1 items-center">
                    <div className="flex flex-col items-center">
                      <div
                        className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold ${
                          isCompleted
                            ? 'bg-teal-600 text-white ring-2 ring-teal-200'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {idx + 1}
                      </div>
                      <span
                        className={`mt-1 text-[11px] font-medium ${
                          isCurrent
                            ? 'text-teal-900 font-semibold'
                            : isCompleted
                            ? 'text-slate-700'
                            : 'text-slate-400'
                        }`}
                      >
                        {step.label}
                      </span>
                    </div>
                    {idx < steps.length - 1 && (
                      <div
                        className={`mx-2 h-0.5 flex-1 ${
                          idx < currentStepIdx ? 'bg-teal-600' : 'bg-slate-200'
                        }`}
                      />
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Image Modal for full view */}
      {showImageModal && imageUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/75 p-4 backdrop-blur-sm"
          onClick={() => setShowImageModal(false)}
        >
          <div
            className="relative max-h-[90vh] max-w-2xl overflow-hidden rounded-xl bg-white p-2 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-2">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-600">
                Complaint Evidence Photo
              </span>
              <button
                type="button"
                onClick={() => setShowImageModal(false)}
                className="rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-2">
              <img
                src={imageUrl}
                alt="Full resolution issue evidence"
                className="max-h-[75vh] w-full rounded-lg object-contain"
              />
            </div>
          </div>
        </div>
      )}
    </>
  )
}

