import { useState } from 'react'
import StatusBadge from '../common/StatusBadge.jsx'

export default function ComplaintSuccessModal({ complaint, onClose, onViewList }) {
  const [copied, setCopied] = useState(false)

  if (!complaint) return null

  function handleCopyId() {
    if (complaint.id) {
      navigator.clipboard.writeText(String(complaint.id))
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-navy-950/75 p-4 backdrop-blur-sm">
      <div className="card w-full max-w-md overflow-hidden bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* Success Icon */}
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-50 text-green-600 ring-4 ring-green-100">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M5 13l4 4L19 7"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>

        {/* Title */}
        <div className="mt-4 text-center">
          <h3 className="font-display text-xl font-bold text-navy-900">
            Complaint Submitted Successfully!
          </h3>
          <p className="mt-1 text-xs text-slate-500">
            Your civic complaint has been registered in the system and routed for inspection.
          </p>
        </div>

        {/* Details Box */}
        <div className="mt-5 rounded-lg border border-slate-200 bg-slate-50 p-4 text-left">
          <div className="flex items-center justify-between border-b border-slate-200/80 pb-2.5">
            <span className="text-xs font-semibold text-slate-500">Complaint ID</span>
            <div className="flex items-center gap-1.5">
              <span className="font-mono text-xs font-bold text-navy-900">
                {complaint.id ? String(complaint.id).slice(0, 13) + '…' : 'N/A'}
              </span>
              <button
                type="button"
                onClick={handleCopyId}
                className="rounded border border-slate-200 bg-white px-1.5 py-0.5 text-[10px] font-semibold text-slate-600 hover:bg-slate-50 hover:text-teal-700"
                title="Copy Full ID"
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between border-b border-slate-200/80 py-2.5">
            <span className="text-xs font-semibold text-slate-500">Initial Status</span>
            <StatusBadge status={complaint.status || 'SUBMITTED'} />
          </div>

          <div className="pt-2.5">
            <span className="text-xs font-semibold text-slate-500">Description</span>
            <p className="mt-1 line-clamp-2 text-xs text-slate-700">
              {complaint.description}
            </p>
          </div>
        </div>

        {/* Notification Note */}
        <p className="mt-4 text-center text-[11px] text-slate-400">
          You can track live resolution progress anytime under the <strong className="text-slate-600">My Complaints</strong> tab.
        </p>

        {/* Action Buttons */}
        <div className="mt-6 flex flex-col gap-2 sm:flex-row">
          <button
            type="button"
            onClick={onViewList}
            className="btn-primary flex-1 text-xs"
          >
            Track in My Complaints
          </button>
          <button
            type="button"
            onClick={onClose}
            className="btn-secondary flex-1 text-xs"
          >
            File Another Issue
          </button>
        </div>
      </div>
    </div>
  )
}

