import { Link } from 'react-router-dom'
import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'
import CivicGuidelines from '../../components/complaint/CivicGuidelines.jsx'

export default function CivicGuidelinesPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans text-slate-800 antialiased">
      <HomeNavbar />

      <main className="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12 w-full">
        {/* Breadcrumb Navigation */}
        <nav className="flex items-center gap-2 text-xs font-medium text-slate-500 mb-6">
          <Link to="/dashboard" className="hover:text-emerald-700 transition-colors">
            Home
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">Civic Guidelines</span>
        </nav>

        <CivicGuidelines />
      </main>

      <HomeFooter />
    </div>
  )
}

