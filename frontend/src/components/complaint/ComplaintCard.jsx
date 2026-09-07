import { useState } from 'react'
import StatusBadge from '../common/StatusBadge.jsx'

export default function ComplaintCard({ complaint }) {
  const [showImageModal, setShowImageModal] = useState(false)
  const [copied, setCopied] = useState(false)

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

  // Formatted date
  const formattedDate = createdAt
    ? new Date(createdAt).toLocaleString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : 'Recently'

  const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`

  const refCode = id ? String(id).slice(0, 8).toUpperCase() : 'NEW'

  function handleCopyRef() {
    if (id) {
      navigator.clipboard.writeText(String(id))
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // 4-Stage Resolution Pipeline
  const steps = [
    { key: 'SUBMITTED', label: 'Reported', desc: 'Logged in system' },
    { key: 'IN_PROGRESS', label: 'Under Action', desc: 'Team dispatched' },
    { key: 'RESOLVED', label: 'Resolved', desc: 'Verified fixed' },
  ]

  function getStepIndex(currentStatus) {
    if (currentStatus === 'RESOLVED' || currentStatus === 'CLOSED') return 2
    if (currentStatus === 'IN_PROGRESS') return 1
    return 0
  }

  const currentStepIdx = getStepIndex(status)

  return (
    <>
      <div className="card overflow-hidden transition-all duration-200 hover:border-slate-300 hover:shadow-elevated">
        <div className="p-4 sm:p-6">
          {/* Header Row */}
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleCopyRef}
                title="Click to copy full ID"
                className="group inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 font-mono text-xs font-bold text-slate-700 hover:bg-slate-200"
              >
                <span>#{refCode}</span>
                <span className="text-[10px] text-slate-400 group-hover:text-slate-600">
                  {copied ? '✓ Copied' : '📋'}
                </span>
              </button>
              <span className="text-slate-300">·</span>
              <span className="text-xs text-slate-500">{formattedDate}</span>
            </div>

            <StatusBadge status={status} />
          </div>

          {/* Main Body */}
          <div className="mt-4 flex flex-col-reverse sm:flex-row sm:items-start gap-4 justify-between">
            <div className="flex-1 space-y-3">
              <p className="text-sm leading-relaxed text-slate-800 whitespace-pre-wrap">
                {description}
              </p>

              {/* Location & Maps info */}
              <div className="space-y-1.5 pt-1">
                {address && (
                  <div className="flex items-center gap-1.5 text-xs text-slate-600">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-slate-400 shrink-0" aria-hidden="true">
                      <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                    </svg>
                    <span className="font-medium text-slate-800">{address}</span>
                  </div>
                )}

                <div className="flex items-center gap-2 text-xs">
                  <span className="inline-flex items-center gap-1 text-slate-500 font-mono">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-teal-600" aria-hidden="true">
                      <path d="M12 2a8 8 0 00-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 00-8-8z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                    {latitude?.toFixed(4)}, {longitude?.toFixed(4)}
                  </span>
                  <a
                    href={mapsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-0.5 text-xs font-semibold text-teal-700 hover:text-teal-900 hover:underline"
                  >
                    <span>View on Map</span>
                    <span aria-hidden="true">&rarr;</span>
                  </a>
                </div>
              </div>
            </div>

            {/* Evidence Image Thumbnail */}
            {imageUrl && (
              <div className="relative shrink-0 self-start">
                <button
                  type="button"
                  onClick={() => setShowImageModal(true)}
                  className="group relative block h-24 w-24 sm:h-28 sm:w-28 overflow-hidden rounded-xl border border-slate-200 bg-slate-100 shadow-sm transition-transform active:scale-95"
                  title="Tap to zoom evidence photo"
                >
                  <img
                    src={imageUrl}
                    alt="Issue evidence"
                    className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-110"
                    loading="lazy"
                  />
                  <div className="absolute inset-0 flex items-center justify-center bg-navy-900/40 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
                    <span className="rounded-full bg-navy-950/80 px-2.5 py-1 text-[10px] font-bold text-white shadow-sm">
                      🔍 Zoom
                    </span>
                  </div>
                </button>
              </div>
            )}
          </div>

          {/* Progress Timeline Stepper */}
          <div className="mt-5 rounded-xl bg-slate-50 p-3 sm:p-4 border border-slate-100">
            <div className="flex items-center justify-between">
              {steps.map((step, idx) => {
                const isCompleted = idx <= currentStepIdx
                const isCurrent = idx === currentStepIdx

                return (
                  <div key={step.key} className="flex flex-1 items-center">
                    <div className="flex flex-col items-center">
                      <div
                        className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold transition-colors ${
                          isCompleted
                            ? 'bg-teal-600 text-white shadow-sm ring-2 ring-teal-200'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {isCompleted && idx < currentStepIdx ? '✓' : idx + 1}
                      </div>
                      <span
                        className={`mt-1.5 text-center text-[11px] font-medium leading-tight ${
                          isCurrent
                            ? 'font-bold text-teal-900'
                            : isCompleted
                            ? 'text-slate-800'
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

      {/* Lightbox Modal */}
      {showImageModal && imageUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/80 p-4 backdrop-blur-sm animate-in fade-in duration-150"
          onClick={() => setShowImageModal(false)}
        >
          <div
            className="relative max-h-[90vh] max-w-2xl overflow-hidden rounded-2xl bg-white p-2 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-2.5">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-700">
                Civic Issue Photo Proof
              </span>
              <button
                type="button"
                onClick={() => setShowImageModal(false)}
                className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-100 text-slate-600 hover:bg-slate-200"
              >
                ✕
              </button>
            </div>
            <div className="p-2">
              <img
                src={imageUrl}
                alt="Full resolution issue evidence"
                className="max-h-[75vh] w-full rounded-xl object-contain bg-slate-50"
              />
            </div>
          </div>
        </div>
      )}
    </>
  )
}
