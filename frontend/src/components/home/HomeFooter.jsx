import { Link } from 'react-router-dom'
import { LeafLogo } from './HomeIcons.jsx'

export default function HomeFooter() {
  return (
    <footer className="border-t border-slate-200 bg-white pt-12 pb-8 mt-12 text-sm text-slate-600">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Brand Col */}
          <div className="space-y-3 md:col-span-1">
            <Link to="/" className="flex items-center gap-2.5">
              <LeafLogo className="w-7 h-7 text-emerald-700" />
              <span className="font-bold text-lg text-slate-900">CivicSense</span>
            </Link>
            <p className="text-xs text-slate-500 leading-relaxed">
              Empowering citizens to report hazards, corroborate civic reports, and collaborate directly with municipal authorities.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-bold text-slate-900 mb-3 text-xs uppercase tracking-wider">
              Quick Links
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/dashboard" className="hover:text-emerald-700 transition-colors">
                  Home
                </Link>
              </li>
              <li>
                <Link to="/dashboard#community-reports" className="hover:text-emerald-700 transition-colors">
                  Community Feed
                </Link>
              </li>
              <li>
                <Link to="/report-issue" className="hover:text-emerald-700 transition-colors">
                  Report Issue
                </Link>
              </li>
              <li>
                <Link to="/my-reports" className="hover:text-emerald-700 transition-colors">
                  My Reports
                </Link>
              </li>
            </ul>
          </div>

          {/* Resources */}
          <div>
            <h4 className="font-bold text-slate-900 mb-3 text-xs uppercase tracking-wider">
              Resources
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/how-it-works" className="hover:text-emerald-700 transition-colors">
                  How It Works
                </Link>
              </li>
              <li>
                <Link to="/about" className="hover:text-emerald-700 transition-colors">
                  About
                </Link>
              </li>
              <li>
                <Link to="/guidelines" className="hover:text-emerald-700 transition-colors">
                  Guidelines & FAQ
                </Link>
              </li>
            </ul>
          </div>

          {/* Civic Action */}
          <div>
            <h4 className="font-bold text-slate-900 mb-3 text-xs uppercase tracking-wider">
              Civic Mission
            </h4>
            <p className="text-xs text-slate-500 leading-relaxed">
              Empowering citizens with AI and community corroboration to build cleaner, safer, and more accountable cities.
            </p>
          </div>
        </div>

        <div className="border-t border-slate-100 pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-400 gap-4">
          <p>© {new Date().getFullYear()} CivicSense. All rights reserved.</p>
          <p className="text-center sm:text-right font-medium text-slate-500">
            Cleaner Cities. Stronger Communities. A Brighter Tomorrow.
          </p>
        </div>
      </div>
    </footer>
  )
}
