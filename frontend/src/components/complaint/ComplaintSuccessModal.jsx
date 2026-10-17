import { useState, useEffect, useRef } from 'react'
import StatusBadge from '../common/StatusBadge.jsx'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function ComplaintSuccessModal({ complaint, onClose, onViewList }) {
  const [copied, setCopied] = useState(false)
  const modalRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      gsap.from('.success-icon-badge', {
        scale: 0.5,
        opacity: 0,
        duration: 0.5,
        ease: 'back.out(1.5)',
      })
    }, modalRef)

    return () => ctx.revert()
  }, [])

  if (!complaint) return null

  function handleCopyId() {
    if (complaint.id) {
      navigator.clipboard.writeText(String(complaint.id))
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const shortId = complaint.id ? String(complaint.id).slice(0, 8).toUpperCase() : 'NEW'

  return (
    <div
      ref={modalRef}
      className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/80 p-3 sm:p-4 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="card w-full max-w-md overflow-hidden bg-white p-5 sm:p-6 shadow-2xl border border-slate-200 rounded-2xl sm:rounded-3xl animate-in zoom-in-95 duration-200">
        {/* Animated Checkmark Circle */}
        <div className="success-icon-badge mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-teal-50 text-teal-700 ring-8 ring-teal-50/60 shadow-sm">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>

        {/* Title */}
        <div className="mt-4 text-center">
          <h3 className="font-display text-xl font-bold text-navy-900">
            Complaint Registered!
          </h3>
          <p className="mt-1 text-xs text-slate-500 max-w-xs mx-auto">
            Your report has been logged and routed to your municipal ward officer for inspection.
          </p>
        </div>

        {/* Ticket Details Box */}
        <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50/80 p-3.5 sm:p-4 space-y-2.5">
          <div className="flex items-center justify-between border-b border-slate-200/80 pb-2.5">
            <span className="text-xs font-semibold text-slate-500">Reference Token</span>
            <div className="flex items-center gap-1.5">
              <span className="font-mono text-xs font-bold text-navy-900 bg-white px-2 py-0.5 rounded border border-slate-200">
                #{shortId}
              </span>
              <button
                type="button"
                onClick={handleCopyId}
                className="rounded-md border border-slate-200 bg-white px-2 py-0.5 text-[11px] font-semibold text-teal-800 hover:bg-slate-50 active:scale-95 cursor-pointer"
              >
                {copied ? '✓ Copied' : 'Copy'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between border-b border-slate-200/80 pb-2.5">
            <span className="text-xs font-semibold text-slate-500">Current Status</span>
            <StatusBadge status={complaint.status || 'SUBMITTED'} />
          </div>

          <div className="pt-0.5">
            <span className="text-xs font-semibold text-slate-500">Description</span>
            <p className="mt-1 line-clamp-2 text-xs text-slate-700 leading-relaxed">
              {complaint.description}
            </p>
          </div>
        </div>

        {/* Next Steps Notification */}
        <div className="mt-4 rounded-xl bg-teal-50/70 p-3 border border-teal-100 flex items-start gap-2.5">
          <span className="text-teal-700 text-sm">🔔</span>
          <p className="text-[11px] text-teal-950 leading-relaxed">
            You will receive status updates as the ground inspection team is mobilized.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="mt-6 flex flex-col gap-2.5 sm:flex-row">
          <button
            type="button"
            onClick={onViewList}
            className="btn-primary flex-1 min-h-[44px] text-xs py-2.5 font-bold shadow-sm"
          >
            Track in My Reports &rarr;
          </button>
          <button
            type="button"
            onClick={onClose}
            className="btn-secondary flex-1 min-h-[44px] text-xs py-2.5 font-semibold"
          >
            Report Another Issue
          </button>
        </div>
      </div>
    </div>
  )
}
