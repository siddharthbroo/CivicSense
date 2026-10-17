import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

/**
 * Shared shell for every public/unauthenticated screen: login,
 * registration steps, and the success screen. Keeps the civic-tech
 * branding consistent without duplicating markup on every page.
 */
export default function AuthLayout({ eyebrow, title, subtitle, children, width = 'max-w-md' }) {
  const containerRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      gsap.from('.auth-card-wrapper', {
        opacity: 0,
        y: 16,
        duration: 0.5,
        ease: 'power2.out',
      })
    }, containerRef)

    return () => ctx.revert()
  }, [])

  return (
    <div ref={containerRef} className="flex min-h-screen flex-col bg-navy-950">
      <div className="border-b border-white/10">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-2.5 px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-2.5 group">
            <svg width="26" height="26" viewBox="0 0 32 32" aria-hidden="true">
              <rect width="32" height="32" rx="6" fill="#0B1526" />
              <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27C10.8 25.6 7 21 7 15V9.5L16 6Z" fill="#1F9D99" />
              <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27V6Z" fill="#166363" />
            </svg>
            <span className="font-display text-lg font-semibold text-white group-hover:text-teal-300 transition-colors">
              CivicSense
            </span>
          </Link>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center px-3 sm:px-6 py-8 sm:py-12">
        <div className={`auth-card-wrapper w-full ${width}`}>
          <div className="mb-6 text-center">
            {eyebrow && (
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-teal-400">
                {eyebrow}
              </p>
            )}
            <h1 className="text-xl sm:text-2xl font-semibold text-white tracking-tight">{title}</h1>
            {subtitle && <p className="mt-1.5 text-xs sm:text-sm text-slate-400">{subtitle}</p>}
          </div>

          <div className="card p-5 sm:p-8 bg-white shadow-2xl rounded-2xl sm:rounded-3xl border border-slate-200">
            {children}
          </div>
        </div>
      </div>

      <footer className="border-t border-white/10 py-4 text-center text-xs text-slate-500 px-4">
        CivicSense · Smart Civic Complaint Management &amp; Zone Intelligence
      </footer>
    </div>
  )
}
