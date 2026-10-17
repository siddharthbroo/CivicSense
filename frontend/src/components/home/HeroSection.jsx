import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import {
  PlusCircleIcon,
  UsersGroupIcon,
  MegaphoneIcon,
  TrackChartIcon,
  SparklesIcon,
} from './HomeIcons.jsx'
import civicHeroBg from '../../assets/civic-city-hero.png'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function HeroSection() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const sectionRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      // Animate left content items
      gsap.from('.hero-anim-item', {
        opacity: 0,
        y: 20,
        duration: 0.65,
        stagger: 0.12,
        ease: 'power2.out',
      })

      // Animate right floating card
      gsap.from('.hero-card', {
        opacity: 0,
        y: 28,
        scale: 0.96,
        duration: 0.75,
        delay: 0.2,
        ease: 'power2.out',
      })

      // Animate steps inside the right card
      gsap.from('.hero-step-item', {
        opacity: 0,
        x: 12,
        duration: 0.5,
        stagger: 0.08,
        delay: 0.45,
        ease: 'power1.out',
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  function handleReportIssue() {
    if (isAuthenticated) {
      navigate('/report-issue')
    } else {
      navigate('/login', { state: { from: { pathname: '/report-issue' } } })
    }
  }

  function handleExploreCommunity() {
    const el = document.getElementById('community-reports')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <section
      ref={sectionRef}
      className="relative overflow-hidden bg-slate-900 min-h-[560px] lg:min-h-[620px] flex items-center"
    >
      {/* Visual Background: Lush City Skyline Panorama with Soft Overlay */}
      <div
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat transition-transform duration-1000 scale-105"
        style={{
          backgroundImage: `linear-gradient(to right, rgba(15, 23, 42, 0.78) 0%, rgba(15, 23, 42, 0.52) 48%, rgba(15, 23, 42, 0.28) 100%), url(${civicHeroBg})`,
        }}
      >
        {/* Secondary subtle green gradient glow from bottom-left */}
        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/50 via-transparent to-black/20 pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-96 h-96 rounded-full bg-emerald-600/20 blur-3xl pointer-events-none" />
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 sm:py-16 lg:py-24 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-center">
          
          {/* Left Hero Message & Action CTAs */}
          <div className="lg:col-span-7 text-white space-y-5 sm:space-y-7">
            <div className="hero-anim-item inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-xs font-semibold tracking-wide text-emerald-300">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Community-Driven Civic Governance
            </div>

            <h1 className="hero-anim-item text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-white leading-[1.18] sm:leading-[1.15]">
              A Cleaner, Safer City Starts with You
            </h1>

            <p className="hero-anim-item text-base sm:text-xl text-slate-200 max-w-2xl font-normal leading-relaxed">
              Report civic issues. Join your community. Make a real impact.
            </p>

            {/* Primary Action Buttons */}
            <div className="hero-anim-item flex flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-4 pt-2">
              <button
                type="button"
                onClick={handleReportIssue}
                className="inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-base shadow-lg shadow-emerald-950/30 hover:shadow-emerald-600/40 transition-all transform hover:-translate-y-0.5 active:scale-95 cursor-pointer touch-target"
              >
                <PlusCircleIcon className="w-5 h-5 text-white stroke-[2.5]" />
                <span>Report an Issue</span>
              </button>

              <button
                type="button"
                onClick={handleExploreCommunity}
                className="inline-flex items-center justify-center gap-2.5 px-6 py-3.5 rounded-xl bg-black/40 hover:bg-black/60 text-white font-semibold text-base border border-white/30 backdrop-blur-md shadow-md transition-all transform hover:-translate-y-0.5 active:scale-95 cursor-pointer touch-target"
              >
                <UsersGroupIcon className="w-5 h-5 text-white/90" />
                <span>Explore Community</span>
              </button>
            </div>
          </div>

          {/* Right Floating Workflow Card */}
          <div className="lg:col-span-5 flex flex-col items-center lg:items-end justify-center relative">
            <div className="hero-card w-full max-w-sm rounded-3xl bg-white/95 backdrop-blur-xl p-6 sm:p-7 shadow-2xl border border-white/80 transition-all hover:shadow-emerald-900/20">
              <div className="space-y-4 sm:space-y-5">
                {/* 1. Report */}
                <div className="hero-step-item flex items-start gap-3.5 sm:gap-4 group">
                  <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-md group-hover:scale-105 transition-transform">
                    <MegaphoneIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-tight">Report</h3>
                    <p className="text-xs text-slate-500 mt-0.5">Report issues in your area</p>
                  </div>
                </div>

                {/* 2. Engage */}
                <div className="hero-step-item flex items-start gap-3.5 sm:gap-4 group">
                  <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-md group-hover:scale-105 transition-transform">
                    <UsersGroupIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-tight">Engage</h3>
                    <p className="text-xs text-slate-500 mt-0.5">Support issues that matter</p>
                  </div>
                </div>

                {/* 3. Track */}
                <div className="hero-step-item flex items-start gap-3.5 sm:gap-4 group">
                  <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-md group-hover:scale-105 transition-transform">
                    <TrackChartIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-tight">Track</h3>
                    <p className="text-xs text-slate-500 mt-0.5">See real progress</p>
                  </div>
                </div>

                {/* 4. Create Change */}
                <div className="hero-step-item flex items-start gap-3.5 sm:gap-4 group">
                  <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-full bg-purple-600 text-white flex items-center justify-center shrink-0 shadow-md group-hover:scale-105 transition-transform">
                    <SparklesIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-tight">Create Change</h3>
                    <p className="text-xs text-slate-500 mt-0.5">Build a better tomorrow</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Handwritten Script Accent at bottom right */}
            <div className="hidden sm:block mt-4 -mr-2 text-right">
              <span
                className="inline-block text-white/90 text-xl font-semibold tracking-wide transform -rotate-3 select-none drop-shadow-md"
                style={{ fontFamily: 'Caveat, "Brush Script MT", cursive, sans-serif' }}
              >
                Cleaner Cities <br />
                <span className="text-emerald-300">Happier People</span>
              </span>
            </div>
          </div>

        </div>
      </div>
    </section>
  )
}
