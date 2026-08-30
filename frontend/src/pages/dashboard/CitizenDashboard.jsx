import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext.jsx'
import ComplaintStats from '../../components/complaint/ComplaintStats.jsx'
import ComplaintForm from '../../components/complaint/ComplaintForm.jsx'
import ComplaintList from '../../components/complaint/ComplaintList.jsx'
import CivicGuidelines from '../../components/complaint/CivicGuidelines.jsx'
import ComplaintSuccessModal from '../../components/complaint/ComplaintSuccessModal.jsx'

export default function CitizenDashboard() {
  const { user } = useAuth()
  const storageKey = `civicsense_complaints_${user?.mobileNumber || 'citizen'}`

  // Load persisted complaints for this citizen
  const [complaints, setComplaints] = useState(() => {
    try {
      const saved = localStorage.getItem(storageKey)
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  const [activeTab, setActiveTab] = useState('form') // 'form' | 'list' | 'guidelines'
  const [newlyCreatedComplaint, setNewlyCreatedComplaint] = useState(null)

  // Persist complaints on change
  useEffect(() => {
    try {
      localStorage.setItem(storageKey, JSON.stringify(complaints))
    } catch (e) {
      console.error('Failed to persist complaints to localStorage', e)
    }
  }, [complaints, storageKey])

  function handleComplaintCreated(newComplaint) {
    setComplaints((prev) => [newComplaint, ...prev])
    setNewlyCreatedComplaint(newComplaint)
  }

  return (
    <div className="space-y-8">
      {/* Citizen Welcome Banner */}
      <div className="card overflow-hidden bg-gradient-to-r from-navy-900 via-navy-800 to-navy-900 p-6 text-white shadow-elevated sm:p-8">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-2">
              <span className="badge bg-teal-400/20 text-teal-300 ring-1 ring-inset ring-teal-400/30">
                Verified Citizen
              </span>
              <span className="text-xs text-slate-300">· {user?.mobileNumber}</span>
            </div>
            <h1 className="mt-2 font-display text-2xl font-bold tracking-tight text-white sm:text-3xl">
              Namaste, {user?.name || 'Citizen'}
            </h1>
            <p className="mt-1 max-w-2xl text-xs text-slate-300 sm:text-sm">
              Your voice empowers our city. Report civic issues, track real-time resolution progress, and build a cleaner, safer neighborhood.
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <button
              type="button"
              onClick={() => setActiveTab('form')}
              className="btn-accent text-xs font-semibold shadow-sm"
            >
              + File New Complaint
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('list')}
              className="btn-secondary border-slate-700 bg-navy-800/80 text-xs text-slate-200 hover:bg-navy-700"
            >
              My Complaints ({complaints.length})
            </button>
          </div>
        </div>
      </div>

      {/* Metrics & Statistics */}
      <ComplaintStats complaints={complaints} onTabChange={setActiveTab} />

      {/* Main Tabs Navigation */}
      <div className="border-b border-slate-200">
        <nav className="flex space-x-8" aria-label="Dashboard Tabs">
          <button
            type="button"
            onClick={() => setActiveTab('form')}
            className={`border-b-2 py-3 text-sm font-semibold transition-all ${
              activeTab === 'form'
                ? 'border-teal-600 text-teal-700'
                : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700'
            }`}
          >
            File a Complaint
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('list')}
            className={`flex items-center gap-2 border-b-2 py-3 text-sm font-semibold transition-all ${
              activeTab === 'list'
                ? 'border-teal-600 text-teal-700'
                : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700'
            }`}
          >
            <span>My Complaints</span>
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                activeTab === 'list'
                  ? 'bg-teal-100 text-teal-800'
                  : 'bg-slate-100 text-slate-600'
              }`}
            >
              {complaints.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('guidelines')}
            className={`border-b-2 py-3 text-sm font-semibold transition-all ${
              activeTab === 'guidelines'
                ? 'border-teal-600 text-teal-700'
                : 'border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700'
            }`}
          >
            Civic Guidelines & Helplines
          </button>
        </nav>
      </div>

      {/* Tab Content Panes */}
      <div>
        {activeTab === 'form' && (
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <ComplaintForm onComplaintCreated={handleComplaintCreated} />
            </div>
            <div className="space-y-6">
              <CivicGuidelinesCompact onLearnMore={() => setActiveTab('guidelines')} />
            </div>
          </div>
        )}

        {activeTab === 'list' && (
          <ComplaintList
            complaints={complaints}
            onNewComplaintClick={() => setActiveTab('form')}
          />
        )}

        {activeTab === 'guidelines' && <CivicGuidelines />}
      </div>

      {/* Submission Success Dialog */}
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

function CivicGuidelinesCompact({ onLearnMore }) {
  return (
    <div className="card space-y-4 p-6">
      <h3 className="font-display text-base font-bold text-navy-900">
        Tips for Effective Reporting
      </h3>
      <ul className="space-y-3 text-xs leading-relaxed text-slate-600">
        <li className="flex items-start gap-2">
          <span className="text-teal-600 font-bold">1.</span>
          <span><strong>Accurate GPS:</strong> Always enable location so municipal teams reach the exact spot.</span>
        </li>
        <li className="flex items-start gap-2">
          <span className="text-teal-600 font-bold">2.</span>
          <span><strong>Clear Proof:</strong> Take daylight photos showing both the defect and nearby surroundings.</span>
        </li>
        <li className="flex items-start gap-2">
          <span className="text-teal-600 font-bold">3.</span>
          <span><strong>Key Landmark:</strong> Mention nearest landmarks like school, bank, or shop gate.</span>
        </li>
      </ul>

      <div className="rounded-md bg-teal-50/70 p-3 ring-1 ring-inset ring-teal-200">
        <p className="text-[11px] text-teal-900">
          Complaints submitted here are directly routed to the municipal ward officer for inspection.
        </p>
      </div>

      <button
        type="button"
        onClick={onLearnMore}
        className="text-xs font-semibold text-teal-700 hover:text-teal-900 hover:underline"
      >
        View full guidelines & emergency helplines &rarr;
      </button>
    </div>
  )
}

