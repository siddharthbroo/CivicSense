import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'
import ComplaintForm from '../../components/complaint/ComplaintForm.jsx'
import ComplaintSuccessModal from '../../components/complaint/ComplaintSuccessModal.jsx'

export default function ReportIssuePage() {
  const navigate = useNavigate()
  const [createdComplaint, setCreatedComplaint] = useState(null)

  function handleComplaintCreated(complaint) {
    setCreatedComplaint(complaint)
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans text-slate-800 antialiased">
      {/* Top Navigation */}
      <HomeNavbar />

      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 w-full">
        {/* Breadcrumb Navigation */}
        <nav className="flex items-center gap-2 text-xs font-medium text-slate-500 mb-6">
          <Link to="/dashboard" className="hover:text-emerald-700 transition-colors">
            Home
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Report an Issue</span>
        </nav>

        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Report a Civic Issue
          </h1>
          <p className="text-sm sm:text-base text-slate-500 mt-1.5 max-w-2xl">
            Submit road damage, sanitation hazards, or broken infrastructure with photo proof and GPS coordinates.
            Your report will be analyzed with Gemini AI, prioritized, and routed to municipal authorities.
          </p>
        </div>

        {/* Complaint Form Card */}
        <div className="bg-white rounded-2xl sm:rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8">
          <ComplaintForm onComplaintCreated={handleComplaintCreated} />
        </div>

        {/* Submission Confirmation Modal */}
        {createdComplaint && (
          <ComplaintSuccessModal
            complaint={createdComplaint}
            onClose={() => setCreatedComplaint(null)}
            onViewList={() => {
              setCreatedComplaint(null)
              navigate('/my-reports')
            }}
          />
        )}
      </main>

      {/* Civic Footer */}
      <HomeFooter />
    </div>
  )
}

