import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { LeafIcon, ArrowRightIcon } from './HomeIcons.jsx'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function BottomCtaSection() {
  const { isAuthenticated } = useAuth()
  const sectionRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      gsap.from('.cta-box', {
        opacity: 0,
        y: 20,
        duration: 0.6,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 90%',
          once: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      <div className="cta-box relative overflow-hidden rounded-3xl bg-emerald-50 border border-emerald-200/70 p-6 sm:p-10 shadow-xs">
        {/* Subtle decorative leaf watermark */}
        <div className="absolute right-32 -bottom-10 w-48 h-48 rounded-full bg-emerald-200/20 blur-2xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Left Message Area with Leaf Icon */}
          <div className="flex items-start sm:items-center gap-4 text-left w-full md:w-auto">
            <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-md">
              <LeafIcon className="w-6 h-6 sm:w-7 sm:h-7" />
            </div>
            <div>
              <h2 className="text-lg sm:text-2xl font-extrabold text-slate-900 tracking-tight">
                Be the Change in Your Community
              </h2>
              <p className="text-xs sm:text-sm text-slate-600 mt-1 max-w-2xl leading-relaxed">
                Take a few minutes to report an issue or support existing reports. Together, we can build better cities.
              </p>
            </div>
          </div>

          {/* Right Action Button */}
          <div className="shrink-0 w-full md:w-auto text-center">
            <Link
              to={isAuthenticated ? '/dashboard' : '/register'}
              className="inline-flex items-center justify-center gap-2.5 w-full sm:w-auto px-7 py-3.5 rounded-xl bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-sm shadow-md hover:shadow-lg transition-all transform hover:-translate-y-0.5 active:scale-95 touch-target"
            >
              <span>Get Started Today</span>
              <ArrowRightIcon className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}
