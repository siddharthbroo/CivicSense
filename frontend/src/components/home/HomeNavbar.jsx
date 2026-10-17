import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { LeafLogo, SearchIcon, MenuIcon, CloseIcon } from './HomeIcons.jsx'

export default function HomeNavbar({ onSearchClick }) {
  const { user, isAuthenticated, logout } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  function handleReportIssue() {
    if (isAuthenticated) {
      navigate('/report-issue')
    } else {
      navigate('/login', { state: { from: { pathname: '/report-issue' } } })
    }
  }

  function handleLogout() {
    logout()
    navigate('/', { replace: true })
  }

  function scrollToSection(sectionId) {
    setMobileMenuOpen(false)
    const isHome = location.pathname === '/' || location.pathname === '/dashboard' || location.pathname === '/home'
    if (!isHome) {
      navigate('/dashboard')
      setTimeout(() => {
        const element = document.getElementById(sectionId)
        if (element) {
          element.scrollIntoView({ behavior: 'smooth' })
        }
      }, 100)
    } else {
      const element = document.getElementById(sectionId)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      }
    }
  }

  const isHomeActive = location.pathname === '/' || location.pathname === '/dashboard' || location.pathname === '/home'
  const isMyReportsActive = location.pathname === '/my-reports'
  const isReportActive = location.pathname === '/report-issue'
  const isHowItWorksActive = location.pathname === '/how-it-works'
  const isAboutActive = location.pathname === '/about'

  return (
    <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-100 transition-all">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          {/* Logo & Branding */}
          <Link to="/dashboard" className="flex items-center gap-3 group shrink-0">
            <div className="p-1.5 rounded-xl bg-emerald-50 text-emerald-600 group-hover:bg-emerald-100 transition-colors">
              <LeafLogo className="w-8 h-8" />
            </div>
            <div className="flex flex-col">
              <span className="font-bold text-lg sm:text-xl tracking-tight text-slate-900 leading-none group-hover:text-emerald-700 transition-colors">
                CivicSense
              </span>
              <span className="hidden sm:block text-[11px] font-medium text-slate-500 tracking-tight mt-0.5">
                Stronger Communities. Safer Cities.
              </span>
            </div>
          </Link>

          {/* Desktop Center Navigation */}
          <nav className="hidden md:flex items-center gap-7 text-sm font-medium text-slate-600">
            <Link
              to="/dashboard"
              className={`relative py-2 transition-colors ${
                isHomeActive ? 'text-emerald-700 font-semibold' : 'hover:text-emerald-700'
              }`}
            >
              Home
              {isHomeActive && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600 rounded-full" />
              )}
            </Link>

            <button
              type="button"
              onClick={() => scrollToSection('community-reports')}
              className="hover:text-emerald-700 transition-colors cursor-pointer py-2"
            >
              Community
            </button>

            <button
              type="button"
              onClick={handleReportIssue}
              className={`hover:text-emerald-700 transition-colors cursor-pointer py-2 ${
                isReportActive ? 'text-emerald-700 font-semibold' : ''
              }`}
            >
              Report Issue
            </button>

            {isAuthenticated && (
              <Link
                to="/my-reports"
                className={`relative py-2 transition-colors ${
                  isMyReportsActive ? 'text-emerald-700 font-semibold' : 'hover:text-emerald-700'
                }`}
              >
                My Reports
                {isMyReportsActive && (
                  <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600 rounded-full" />
                )}
              </Link>
            )}

            <Link
              to="/how-it-works"
              className={`relative py-2 transition-colors ${
                isHowItWorksActive ? 'text-emerald-700 font-semibold' : 'hover:text-emerald-700'
              }`}
            >
              How It Works
              {isHowItWorksActive && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600 rounded-full" />
              )}
            </Link>

            <Link
              to="/about"
              className={`relative py-2 transition-colors ${
                isAboutActive ? 'text-emerald-700 font-semibold' : 'hover:text-emerald-700'
              }`}
            >
              About
              {isAboutActive && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-emerald-600 rounded-full" />
              )}
            </Link>
          </nav>

          {/* Desktop Right Action Area */}
          <div className="hidden md:flex items-center gap-4">
            {/* Search Icon button */}
            <button
              type="button"
              onClick={onSearchClick || (() => scrollToSection('community-reports'))}
              aria-label="Search reports"
              title="Search reports"
              className="p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-full transition-colors cursor-pointer"
            >
              <SearchIcon className="w-5 h-5" />
            </button>

            {isAuthenticated ? (
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2.5 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-100">
                  <div className="w-8 h-8 rounded-full bg-emerald-700 text-white font-bold text-xs flex items-center justify-center shadow-xs">
                    {user?.name ? user.name.charAt(0).toUpperCase() : 'C'}
                  </div>
                  <span className="text-xs font-semibold text-slate-700">
                    {user?.name || 'Citizen'}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all cursor-pointer"
                >
                  Log out
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-semibold text-slate-700 hover:text-slate-900 border border-slate-200 rounded-xl hover:bg-slate-50 transition-all shadow-xs"
                >
                  Sign In
                </Link>

                <Link
                  to="/register"
                  className="px-5 py-2.5 text-sm font-semibold text-white bg-emerald-800 hover:bg-emerald-900 rounded-xl shadow-sm transition-all hover:shadow-md active:scale-95"
                >
                  Get Started
                </Link>
              </div>
            )}
          </div>

          {/* Mobile Menu Toggle Button */}
          <div className="flex items-center gap-2 md:hidden">
            <button
              type="button"
              onClick={onSearchClick || (() => scrollToSection('community-reports'))}
              aria-label="Search"
              className="p-2 text-slate-600 hover:text-slate-900 rounded-lg"
            >
              <SearchIcon className="w-5 h-5" />
            </button>
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Open menu"
              className="p-2 text-slate-600 hover:text-slate-900 rounded-lg cursor-pointer"
            >
              {mobileMenuOpen ? <CloseIcon className="w-6 h-6" /> : <MenuIcon className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-100 bg-white px-4 pt-3 pb-6 space-y-2 shadow-lg animate-in slide-in-from-top-2">
          <Link
            to="/dashboard"
            onClick={() => setMobileMenuOpen(false)}
            className={`flex items-center min-h-[44px] px-3 py-2 rounded-xl text-base font-medium transition-colors ${
              isHomeActive ? 'text-emerald-700 bg-emerald-50' : 'text-slate-700 hover:bg-slate-50'
            }`}
          >
            Home
          </Link>

          <button
            type="button"
            onClick={() => scrollToSection('community-reports')}
            className="w-full flex items-center min-h-[44px] text-left px-3 py-2 rounded-xl text-base font-medium text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Community
          </button>

          <button
            type="button"
            onClick={() => {
              setMobileMenuOpen(false)
              handleReportIssue()
            }}
            className="w-full flex items-center min-h-[44px] text-left px-3 py-2 rounded-xl text-base font-medium text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Report Issue
          </button>

          {isAuthenticated && (
            <Link
              to="/my-reports"
              onClick={() => setMobileMenuOpen(false)}
              className={`flex items-center min-h-[44px] px-3 py-2 rounded-xl text-base font-medium transition-colors ${
                isMyReportsActive ? 'text-emerald-700 bg-emerald-50' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              My Reports
            </Link>
          )}

          <Link
            to="/how-it-works"
            onClick={() => setMobileMenuOpen(false)}
            className={`flex items-center min-h-[44px] px-3 py-2 rounded-xl text-base font-medium transition-colors ${
              isHowItWorksActive ? 'text-emerald-700 bg-emerald-50 font-semibold' : 'text-slate-700 hover:bg-slate-50'
            }`}
          >
            How It Works
          </Link>

          <Link
            to="/about"
            onClick={() => setMobileMenuOpen(false)}
            className={`flex items-center min-h-[44px] px-3 py-2 rounded-xl text-base font-medium transition-colors ${
              isAboutActive ? 'text-emerald-700 bg-emerald-50 font-semibold' : 'text-slate-700 hover:bg-slate-50'
            }`}
          >
            About
          </Link>

          <div className="pt-3 border-t border-slate-100 flex flex-col gap-2">
            {isAuthenticated ? (
              <div className="space-y-2">
                <div className="flex items-center gap-2.5 px-3 py-2 bg-slate-50 rounded-xl">
                  <div className="w-8 h-8 rounded-full bg-emerald-700 text-white font-bold text-xs flex items-center justify-center">
                    {user?.name ? user.name.charAt(0).toUpperCase() : 'C'}
                  </div>
                  <div className="text-xs">
                    <p className="font-semibold text-slate-800">{user?.name || 'Citizen'}</p>
                    <p className="text-slate-500">{user?.mobileNumber}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setMobileMenuOpen(false)
                    handleLogout()
                  }}
                  className="w-full text-center px-4 py-2 text-xs font-semibold text-red-600 hover:bg-red-50 rounded-xl transition-colors"
                >
                  Log out
                </button>
              </div>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center px-4 py-2.5 text-sm font-semibold text-slate-700 border border-slate-200 rounded-xl hover:bg-slate-50"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="w-full text-center px-4 py-2.5 text-sm font-semibold text-white bg-emerald-800 rounded-xl shadow-sm"
                >
                  Get Started
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  )
}
