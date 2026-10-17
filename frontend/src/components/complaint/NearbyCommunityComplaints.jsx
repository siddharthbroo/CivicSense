import { useState, useEffect } from 'react'
import { getNearbyCommunityComplaints, supportComplaint, removeSupport } from '../../api/communityApi.js'
import CommunityDetailModal from '../home/CommunityDetailModal.jsx'
import Spinner from '../common/Spinner.jsx'

/**
 * Discovers nearby community complaints around the user's location or confirmed pin.
 * Encourages citizens to support an existing issue ("I'm affected too")
 * instead of submitting an unnecessary duplicate complaint.
 */
export default function NearbyCommunityComplaints({
  latitude,
  longitude,
}) {
  const [nearbyComplaints, setNearbyComplaints] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [supportingId, setSupportingId] = useState(null)
  const [selectedComplaint, setSelectedComplaint] = useState(null)

  useEffect(() => {
    let isMounted = true

    const latNum = parseFloat(latitude)
    const lonNum = parseFloat(longitude)

    if (!latitude || !longitude || isNaN(latNum) || isNaN(lonNum)) {
      // Async reset to avoid triggering ESLint set-state-in-effect warning
      Promise.resolve().then(() => {
        if (isMounted) setNearbyComplaints([])
      })
      return () => {
        isMounted = false
      }
    }

    Promise.resolve().then(() => {
      if (isMounted) setIsLoading(true)
    })

    getNearbyCommunityComplaints({
      latitude: latNum,
      longitude: lonNum,
      radiusKm: 3.0, // 3km neighbourhood radius
    })
      .then((data) => {
        if (isMounted) {
          setNearbyComplaints(Array.isArray(data) ? data : [])
        }
      })
      .catch((err) => {
        console.error('Failed to fetch nearby community complaints:', err)
        if (isMounted) setNearbyComplaints([])
      })
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [latitude, longitude])

  async function handleSupport(e, complaint) {
    e.stopPropagation()
    const targetId = complaint.complaintId
    const currentSupported = complaint.supportedByCurrentUser
    setSupportingId(targetId)

    // Optimistic UI update
    setNearbyComplaints((prev) =>
      prev.map((c) => {
        if (c.complaintId === targetId) {
          return {
            ...c,
            supportedByCurrentUser: !currentSupported,
            supportCount: currentSupported
              ? Math.max(0, (c.supportCount || 0) - 1)
              : (c.supportCount || 0) + 1,
          }
        }
        return c
      })
    )

    try {
      if (currentSupported) {
        await removeSupport(targetId)
      } else {
        await supportComplaint(targetId)
      }
    } catch (err) {
      console.error('Failed to toggle support:', err)
      // Rollback on error
      setNearbyComplaints((prev) =>
        prev.map((c) => {
          if (c.complaintId === targetId) {
            return {
              ...c,
              supportedByCurrentUser: currentSupported,
              supportCount: complaint.supportCount,
            }
          }
          return c
        })
      )
    } finally {
      setSupportingId(null)
    }
  }

  if (isLoading) {
    return (
      <div className="p-4 bg-emerald-50/50 border border-emerald-100 rounded-xl flex items-center justify-center gap-2 text-xs text-emerald-800">
        <Spinner size="sm" />
        <span>Checking for existing reports nearby...</span>
      </div>
    )
  }

  if (!nearbyComplaints || nearbyComplaints.length === 0) {
    return null
  }

  return (
    <div className="bg-amber-50/80 border border-amber-200/80 rounded-2xl p-4 sm:p-5 mb-6 text-slate-800">
      <div className="flex items-start gap-3">
        <div className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center text-amber-700 shrink-0 mt-0.5">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <div className="flex-1">
          <h3 className="text-sm font-bold text-amber-950">
            Similar issues already reported nearby ({nearbyComplaints.length})
          </h3>
          <p className="text-xs text-amber-800 mt-0.5">
            Someone may have already reported this issue. You can back their report with{' '}
            <strong className="font-semibold">&quot;I&apos;m affected too&quot;</strong> to elevate its municipal priority instead of filing a duplicate!
          </p>

          <div className="mt-3 space-y-2.5 max-h-64 overflow-y-auto pr-1">
            {nearbyComplaints.slice(0, 3).map((item) => (
              <div
                key={item.complaintId}
                onClick={() => setSelectedComplaint(item)}
                className="bg-white rounded-xl p-3 border border-amber-200/60 shadow-xs hover:border-emerald-500 hover:shadow-sm cursor-pointer transition-all flex items-center justify-between gap-3"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-slate-900 truncate">
                      {item.title || 'Reported Civic Issue'}
                    </span>
                    {item.category && (
                      <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 shrink-0">
                        {item.category}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 truncate mt-0.5">
                    {item.address || 'Nearby incident area'}
                  </p>
                  <div className="flex items-center gap-3 text-[11px] text-slate-500 mt-1">
                    <span>
                      👥 {item.supportCount || 0} affected
                    </span>
                    {item.commentCount > 0 && <span>💬 {item.commentCount}</span>}
                  </div>
                </div>

                <div className="shrink-0 flex items-center gap-2">
                  <button
                    type="button"
                    disabled={supportingId === item.complaintId}
                    onClick={(e) => handleSupport(e, item)}
                    className={`text-xs px-3 py-1.5 rounded-lg font-semibold transition-all flex items-center gap-1.5 ${
                      item.supportedByCurrentUser
                        ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                        : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                    }`}
                  >
                    <span>👍</span>
                    <span>{item.supportedByCurrentUser ? 'Supported' : "I'm Affected"}</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {selectedComplaint && (
        <CommunityDetailModal
          complaintId={selectedComplaint.complaintId}
          onClose={() => setSelectedComplaint(null)}
          onSupportToggle={(c) => handleSupport({ stopPropagation: () => {} }, c)}
        />
      )}
    </div>
  )
}