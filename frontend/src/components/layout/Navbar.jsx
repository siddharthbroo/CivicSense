import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'

export default function Navbar() {
  const { user, role, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-slate-200 bg-navy-900">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to="/dashboard" className="flex items-center gap-2.5">
          <ShieldMark />
          <span className="font-display text-lg font-semibold text-white">CivicSense</span>
        </Link>

        <div className="flex items-center gap-4">
          {role && (
            <span className="badge bg-teal-500/15 text-teal-300 ring-1 ring-inset ring-teal-400/30">
              {role}
            </span>
          )}
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium text-white">{user?.name}</p>
            <p className="text-xs text-slate-400">{user?.mobileNumber}</p>
          </div>
          <button
            onClick={handleLogout}
            className="btn-secondary border-slate-600 bg-transparent text-slate-200 hover:bg-navy-800"
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}

function ShieldMark() {
  return (
    <svg width="28" height="28" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="6" fill="#0B1526" />
      <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27C10.8 25.6 7 21 7 15V9.5L16 6Z" fill="#1F9D99" />
      <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27V6Z" fill="#166363" />
    </svg>
  )
}
