import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'
import {
  DocumentIcon,
  RobotIcon,
  UsersGroupIcon,
  BuildingPillarIcon,
  CheckCircleIcon,
  ArrowRightIcon,
  ShieldCheckIcon,
} from '../../components/home/HomeIcons.jsx'

import hiwHero from '../../assets/reference-assets/how_it_works_hero.png'
import hiwTech from '../../assets/reference-assets/technology_people.png'
import action1 from '../../assets/reference-assets/action_1_report.png'
import action2 from '../../assets/reference-assets/action_2_ai.png'
import action3 from '../../assets/reference-assets/action_3_community.png'
import action4 from '../../assets/reference-assets/action_4_resolved.png'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function HowItWorksPage() {
  const navigate = useNavigate()
  const pageRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      // Hero entrance
      gsap.from('.hiw-hero-content', {
        opacity: 0,
        y: 20,
        duration: 0.6,
        ease: 'power2.out',
      })

      gsap.from('.hiw-hero-image', {
        opacity: 0,
        scale: 0.96,
        duration: 0.7,
        delay: 0.15,
        ease: 'power2.out',
      })

      // 5-step process cards entrance
      gsap.from('.hiw-step-card', {
        opacity: 0,
        y: 18,
        stagger: 0.08,
        duration: 0.5,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: '#process-section',
          start: 'top 85%',
          once: true,
        },
      })

      // Action cards entrance
      gsap.from('.hiw-action-card', {
        opacity: 0,
        y: 16,
        stagger: 0.08,
        duration: 0.5,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: '.hiw-action-section',
          start: 'top 85%',
          once: true,
        },
      })
    }, pageRef)

    return () => ctx.revert()
  }, [])

  function scrollToProcess() {
    const el = document.getElementById('process-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  const steps = [
    {
      num: 1,
      title: 'Report an Issue',
      desc: 'Snap a photo, add details, and submit your concern in seconds.',
      badgeBg: 'bg-emerald-700 text-white',
      iconBg: 'bg-emerald-50 text-emerald-700',
      icon: <DocumentIcon className="w-6 h-6" />,
    },
    {
      num: 2,
      title: 'AI Analysis',
      desc: 'Our AI analyzes and categorizes the issue automatically.',
      badgeBg: 'bg-blue-600 text-white',
      iconBg: 'bg-blue-50 text-blue-600',
      icon: <RobotIcon className="w-6 h-6" />,
    },
    {
      num: 3,
      title: 'Community Support',
      desc: 'Other citizens can support the issue and add more information.',
      badgeBg: 'bg-amber-500 text-white',
      iconBg: 'bg-amber-50 text-amber-600',
      icon: <UsersGroupIcon className="w-6 h-6" />,
    },
    {
      num: 4,
      title: 'Authority Review',
      desc: 'Verified issues are structured for municipal and community review.',
      badgeBg: 'bg-purple-600 text-white',
      iconBg: 'bg-purple-50 text-purple-600',
      icon: <BuildingPillarIcon className="w-6 h-6" />,
    },
    {
      num: 5,
      title: 'Real Change',
      desc: 'Track progress and see the issue resolved in your neighborhood.',
      badgeBg: 'bg-emerald-600 text-white',
      iconBg: 'bg-emerald-50 text-emerald-700',
      icon: <CheckCircleIcon className="w-6 h-6" />,
    },
  ]

  const actionCards = [
    {
      img: action1,
      title: '1. Report the Issue',
      subtitle: 'Capture and submit in seconds',
    },
    {
      img: action2,
      title: '2. AI Analysis',
      subtitle: 'Automatic categorization',
    },
    {
      img: action3,
      title: '3. Community Support',
      subtitle: 'Citizens join to amplify',
    },
    {
      img: action4,
      title: '4. Issue Resolved',
      subtitle: 'Track the real impact',
    },
  ]

  return (
    <div
      ref={pageRef}
      className="min-h-screen bg-slate-25 flex flex-col font-sans text-slate-800 antialiased selection:bg-emerald-100 selection:text-emerald-900"
    >
      {/* 1. Header Navigation */}
      <HomeNavbar />

      <main className="flex-1 space-y-14 sm:space-y-20 py-6 sm:py-12">
        {/* ================================================================ */}
        {/* HERO SECTION                                                     */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
            {/* Left Copy */}
            <div className="hiw-hero-content lg:col-span-6 space-y-4 sm:space-y-5 text-left">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200/60">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600" />
                HOW IT WORKS
              </span>

              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-900 tracking-tight leading-tight">
                From a Report to a Better City
              </h1>

              <p className="text-sm sm:text-base text-slate-600 leading-relaxed max-w-xl font-normal">
                CivicSense makes it simple for citizens to report issues, get them analyzed with AI, and drive real change in their communities.
              </p>

              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-3.5 pt-2">
                <button
                  type="button"
                  onClick={() => navigate('/report-issue')}
                  className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-xs sm:text-sm shadow-md transition-all active:scale-95 cursor-pointer touch-target"
                >
                  <span>Report an Issue Now</span>
                  <ArrowRightIcon className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={scrollToProcess}
                  className="inline-flex items-center justify-center gap-2 px-5 py-3.5 rounded-2xl bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 font-bold text-xs sm:text-sm shadow-2xs transition-all active:scale-95 cursor-pointer touch-target"
                >
                  <span className="w-4 h-4 rounded-full bg-slate-900 text-white flex items-center justify-center text-[10px]">
                    ▶
                  </span>
                  <span>Explore Process</span>
                </button>
              </div>
            </div>

            {/* Right Hero Image (Exact reference asset) */}
            <div className="hiw-hero-image lg:col-span-6 flex justify-center">
              <div className="relative w-full max-w-lg rounded-3xl overflow-hidden shadow-xl border border-slate-100 bg-emerald-50/40">
                <img
                  src={hiwHero}
                  alt="CivicSense mobile app reporting with city skyline"
                  className="w-full h-auto object-cover"
                />
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================ */}
        {/* THE PROCESS SECTION                                              */}
        {/* ================================================================ */}
        <section id="process-section" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-8 sm:mb-12 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              The Process
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 font-medium">
              A simple 5-step journey from your report to real impact.
            </p>
          </div>

          {/* 5-Step Process: Horizontal on desktop, responsive grid on mobile */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-6 relative">
            {steps.map((step, idx) => (
              <div
                key={step.num}
                className="hiw-step-card bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-5 sm:p-6 shadow-2xs hover:shadow-md transition-all duration-200 flex flex-col items-center text-center space-y-3.5 sm:space-y-4 relative group"
              >
                {/* Step Number Badge */}
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-black shadow-xs ${step.badgeBg}`}
                >
                  {step.num}
                </div>

                {/* Step Icon */}
                <div
                  className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl flex items-center justify-center ${step.iconBg}`}
                >
                  {step.icon}
                </div>

                {/* Step Details */}
                <div className="space-y-1 sm:space-y-1.5">
                  <h3 className="text-sm sm:text-base font-extrabold text-slate-900 tracking-tight">
                    {step.title}
                  </h3>
                  <p className="text-xs text-slate-500 leading-relaxed">
                    {step.desc}
                  </p>
                </div>

                {/* Subtle connecting indicator on desktop */}
                {idx < steps.length - 1 && (
                  <div className="hidden lg:block absolute -right-3 top-1/2 -translate-y-1/2 text-slate-300 font-black text-sm z-10">
                    →
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* ================================================================ */}
        {/* TECHNOLOGY + PEOPLE SECTION                                      */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-white rounded-3xl border border-slate-150 p-6 sm:p-10 lg:p-12 shadow-2xs">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
              {/* Left Image (Exact reference asset) */}
              <div className="lg:col-span-5 rounded-2xl overflow-hidden bg-slate-50 border border-slate-100 shadow-sm flex items-center justify-center">
                <img
                  src={hiwTech}
                  alt="City park with green trees and urban skyline"
                  className="w-full h-auto object-cover"
                />
              </div>

              {/* Right Content */}
              <div className="lg:col-span-7 space-y-4 sm:space-y-5 text-left">
                <span className="inline-flex items-center px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200/60">
                  POWERED BY AI AND COMMUNITY
                </span>

                <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight leading-tight">
                  Technology + People for Stronger Cities
                </h2>

                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  CivicSense combines the power of artificial intelligence with community participation to identify real issues, prioritize critical problems, and ensure they reach the right authorities faster.
                </p>

                {/* 3 Value Pillars */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 space-y-1 text-left">
                    <div className="text-amber-500 font-bold text-sm flex items-center gap-1.5">
                      <span>⚡</span>
                      <span className="text-slate-900 font-bold text-xs">Faster Detection</span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-tight">
                      Instant categorization and authenticity verification.
                    </p>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 space-y-1 text-left">
                    <div className="text-emerald-600 font-bold text-sm flex items-center gap-1.5">
                      <ShieldCheckIcon className="w-4 h-4" />
                      <span className="text-slate-900 font-bold text-xs">More Transparency</span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-tight">
                      Open community tracking and real-time status updates.
                    </p>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 space-y-1 text-left">
                    <div className="text-blue-600 font-bold text-sm flex items-center gap-1.5">
                      <UsersGroupIcon className="w-4 h-4" />
                      <span className="text-slate-900 font-bold text-xs">Stronger Cities</span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-tight">
                      Civic collaboration and neighbor support.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================ */}
        {/* SEE IT IN ACTION SECTION                                         */}
        {/* ================================================================ */}
        <section className="hiw-action-section max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-6 sm:mb-10 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              See It in Action
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 font-medium">
              Watch how a simple report can make a big difference.
            </p>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-6">
            {actionCards.map((card) => (
              <div
                key={card.title}
                className="hiw-action-card bg-white rounded-2xl sm:rounded-3xl border border-slate-150 overflow-hidden shadow-2xs hover:shadow-md transition-all duration-200 flex flex-col group"
              >
                {/* Thumbnail with Play Overlay */}
                <div className="relative h-28 sm:h-36 w-full bg-slate-100 overflow-hidden">
                  <img
                    src={card.img}
                    alt={card.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute inset-0 bg-slate-950/25 flex items-center justify-center">
                    <div className="w-8 h-8 rounded-full bg-white/90 text-slate-900 flex items-center justify-center text-xs font-bold shadow-md group-hover:scale-110 transition-transform">
                      ▶
                    </div>
                  </div>
                </div>

                {/* Card Text */}
                <div className="p-3 sm:p-4 space-y-0.5 sm:space-y-1 text-left">
                  <h4 className="text-xs sm:text-sm font-bold text-slate-900 leading-snug">
                    {card.title}
                  </h4>
                  <p className="text-[10px] sm:text-xs text-slate-500 font-medium truncate">
                    {card.subtitle}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* ================================================================ */}
        {/* FINAL GREEN CTA SECTION                                          */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-emerald-800 rounded-3xl p-6 sm:p-12 lg:p-14 text-center text-white relative overflow-hidden shadow-xl space-y-4 sm:space-y-5">
            {/* Background Accent Rings */}
            <div className="absolute -top-16 -left-16 w-48 h-48 rounded-full bg-emerald-700/40 pointer-events-none" />
            <div className="absolute -bottom-16 -right-16 w-48 h-48 rounded-full bg-emerald-700/40 pointer-events-none" />

            <div className="relative z-10 max-w-xl mx-auto space-y-2.5 sm:space-y-3">
              <span className="text-[10px] sm:text-[11px] font-extrabold uppercase tracking-widest text-emerald-200">
                READY TO MAKE A DIFFERENCE?
              </span>

              <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black tracking-tight text-white">
                Report an Issue Today
              </h2>

              <p className="text-xs sm:text-sm text-emerald-100 font-normal leading-relaxed">
                Join thousands of citizens in building cleaner, safer and happier cities.
              </p>

              <div className="pt-2 sm:pt-3">
                <button
                  type="button"
                  onClick={() => navigate('/report-issue')}
                  className="inline-flex items-center gap-2 px-6 sm:px-7 py-3 sm:py-3.5 rounded-2xl bg-white hover:bg-emerald-50 text-emerald-900 font-extrabold text-xs sm:text-sm shadow-lg transition-all active:scale-95 cursor-pointer touch-target"
                >
                  <span>Report an Issue Now</span>
                  <ArrowRightIcon className="w-4 h-4 text-emerald-900" />
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <HomeFooter />
    </div>
  )
}
