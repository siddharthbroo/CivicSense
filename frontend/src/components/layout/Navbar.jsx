import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'

export default function Navbar() {
  const { user, role, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  // User initial for avatar
  const initial = user?.name ? user.name.charAt(0).toUpperCase() : 'C'

  return (
    <header className="sticky top-0 z-40 border-b border-navy-800/80 bg-navy-950/95 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Brand Logo & Name */}
        <Link to="/dashboard" className="flex items-center gap-2.5 group">
          <ShieldMark />
          <div className="flex flex-col">
            <span className="font-display text-lg font-bold tracking-tight text-white group-hover:text-teal-300 transition-colors">
              CivicSense
            </span>
            <span className="hidden sm:block text-[10px] uppercase font-bold tracking-wider text-teal-400/80 -mt-1">
              National Civic Platform
            </span>
          </div>
        </Link>

        {/* User Profile & Actions */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* User Avatar & Name */}
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-teal-700 to-teal-500 font-display font-bold text-white shadow-sm ring-1 ring-white/20">
              {initial}
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-semibold text-white leading-tight">
                {user?.name || 'Citizen'}
              </p>
              <div className="flex items-center gap-1.5 mt-0.5">
                {role && (
                  <span className="inline-flex items-center rounded-full bg-teal-500/20 px-1.5 py-0.2 text-[9px] font-bold text-teal-300 ring-1 ring-inset ring-teal-400/30">
                    {role}
                  </span>
                )}
                <span className="text-[10px] text-slate-400">
                  {user?.mobileNumber}
                </span>
              </div>
            </div>
          </div>

          {/* Logout Button */}
          <button
            type="button"
            onClick={handleLogout}
            title="Sign out of CivicSense"
            className="btn-secondary border-slate-700/80 bg-navy-900/90 text-xs font-semibold text-slate-300 hover:bg-navy-800 hover:text-white px-3 py-1.5 rounded-xl transition-all active:scale-95"
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
    <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true" className="shrink-0 drop-shadow-sm">
      <rect width="32" height="32" rx="8" fill="#0B1526" />
      <path d="M16 5L26 9V15.5C26 22 21.8 26.8 16 28.5C10.2 26.8 6 22 6 15.5V9L16 5Z" fill="#1F9D99" />
      <path d="M16 5L26 9V15.5C26 22 21.8 26.8 16 28.5V5Z" fill="#166363" />
      <path d="M12 15.5L15 18.5L20 13" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

