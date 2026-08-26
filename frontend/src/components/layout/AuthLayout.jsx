/**
 * Shared shell for every public/unauthenticated screen: login,
 * registration steps, and the success screen. Keeps the civic-tech
 * branding consistent without duplicating markup on every page.
 */
export default function AuthLayout({ eyebrow, title, subtitle, children, width = 'max-w-md' }) {
  return (
    <div className="flex min-h-screen flex-col bg-navy-950">
      <div className="border-b border-white/10">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-2.5 px-4 sm:px-6 lg:px-8">
          <svg width="26" height="26" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="6" fill="#0B1526" />
            <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27C10.8 25.6 7 21 7 15V9.5L16 6Z" fill="#1F9D99" />
            <path d="M16 6L25 9.5V15C25 21 21.2 25.6 16 27V6Z" fill="#166363" />
          </svg>
          <span className="font-display text-lg font-semibold text-white">CivicSense</span>
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center px-4 py-10 sm:px-6">
        <div className={`w-full ${width}`}>
          <div className="mb-6 text-center">
            {eyebrow && (
              <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-teal-400">
                {eyebrow}
              </p>
            )}
            <h1 className="text-2xl font-semibold text-white">{title}</h1>
            {subtitle && <p className="mt-2 text-sm text-slate-400">{subtitle}</p>}
          </div>

          <div className="card p-6 sm:p-8">{children}</div>
        </div>
      </div>

      <footer className="border-t border-white/10 py-4 text-center text-xs text-slate-500">
        CivicSense · Smart Civic Complaint Management &amp; Zone Intelligence
      </footer>
    </div>
  )
}
