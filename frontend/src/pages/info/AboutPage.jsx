import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'
import {
  LeafIcon,
  ShieldCheckIcon,
  UsersGroupIcon,
  ChartBarIcon,
  TargetIcon,
  EyeIcon,
  HeartIcon,
  LightBulbIcon,
  HandshakeIcon,
  ArrowRightIcon,
  LeafLogo,
} from '../../components/home/HomeIcons.jsx'

import aboutHero from '../../assets/reference-assets/about_hero.png'
import aboutStory from '../../assets/reference-assets/about_story.png'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function AboutPage() {
  const navigate = useNavigate()
  const pageRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      // Hero content
      gsap.from('.about-hero-content', {
        opacity: 0,
        y: 20,
        duration: 0.6,
        ease: 'power2.out',
      })

      // Hero image
      gsap.from('.about-hero-image', {
        opacity: 0,
        scale: 0.96,
        duration: 0.7,
        delay: 0.15,
        ease: 'power2.out',
      })

      // Impact strip cards
      gsap.from('.about-impact-card', {
        opacity: 0,
        y: 18,
        stagger: 0.08,
        duration: 0.5,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: '.about-impact-section',
          start: 'top 85%',
          once: true,
        },
      })

      // Story section
      gsap.from('.about-story-card', {
        opacity: 0,
        y: 20,
        duration: 0.6,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: '.about-story-card',
          start: 'top 85%',
          once: true,
        },
      })
    }, pageRef)

    return () => ctx.revert()
  }, [])

  function scrollToMission() {
    const el = document.getElementById('mission-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  const impactStrip = [
    {
      title: 'Cleaner Environment',
      icon: <LeafIcon className="w-6 h-6 text-emerald-700" />,
    },
    {
      title: 'Safer Communities',
      icon: <ShieldCheckIcon className="w-6 h-6 text-emerald-700" />,
    },
    {
      title: 'More Transparency',
      icon: <UsersGroupIcon className="w-6 h-6 text-emerald-700" />,
    },
    {
      title: 'Greater Civic Participation',
      icon: <ChartBarIcon className="w-6 h-6 text-emerald-700" />,
    },
  ]

  const teamValues = [
    {
      title: 'Citizen-First',
      desc: 'Everything we do is for the citizens.',
      iconBg: 'bg-emerald-50 text-emerald-700 border-emerald-100',
      icon: <UsersGroupIcon className="w-6 h-6" />,
    },
    {
      title: 'Innovation',
      desc: 'Using AI to solve real-world problems.',
      iconBg: 'bg-amber-50 text-amber-600 border-amber-100',
      icon: <LightBulbIcon className="w-6 h-6" />,
    },
    {
      title: 'Impact',
      desc: 'Turning reports into real change.',
      iconBg: 'bg-teal-50 text-teal-700 border-teal-100',
      icon: <HandshakeIcon className="w-6 h-6" />,
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
            {/* Left Hero Content */}
            <div className="about-hero-content lg:col-span-6 space-y-4 sm:space-y-5 text-left">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200/60">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600" />
                ABOUT CIVICSENSE
              </span>

              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black text-slate-900 tracking-tight leading-tight">
                Building Better Cities Together
              </h1>

              <p className="text-sm sm:text-base text-slate-600 leading-relaxed max-w-xl font-normal">
                CivicSense is a citizen-first platform that uses technology and community participation to make our cities cleaner, safer and more livable.
              </p>

              <div className="pt-2">
                <button
                  type="button"
                  onClick={scrollToMission}
                  className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-xs sm:text-sm shadow-md transition-all active:scale-95 cursor-pointer touch-target"
                >
                  <span>Our Mission</span>
                  <span className="text-sm">↓</span>
                </button>
              </div>
            </div>

            {/* Right Hero Image (Exact reference asset) */}
            <div className="about-hero-image lg:col-span-6 flex justify-center">
              <div className="relative w-full max-w-lg rounded-3xl overflow-hidden shadow-xl border border-slate-100 bg-emerald-50/40">
                <img
                  src={aboutHero}
                  alt="City skyline with green trees and urban development"
                  className="w-full h-auto object-cover"
                />
                <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-md px-3.5 py-2 rounded-2xl shadow-sm border border-slate-100 text-right hidden sm:block">
                  <p className="text-[11px] font-black text-emerald-800 leading-tight">
                    Cleaner Cities
                  </p>
                  <p className="text-[10px] font-bold text-slate-600 leading-tight">
                    Stronger Communities
                  </p>
                  <p className="text-[9px] font-medium text-slate-400 leading-tight">
                    A Brighter Tomorrow
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================ */}
        {/* IMPACT / VALUES STRIP                                            */}
        {/* ================================================================ */}
        <section className="about-impact-section max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3.5 sm:gap-6">
            {impactStrip.map((item) => (
              <div
                key={item.title}
                className="about-impact-card bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-4 sm:p-6 shadow-2xs hover:shadow-md transition-all duration-200 flex flex-col items-center text-center space-y-2.5 sm:space-y-3"
              >
                <div className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center shrink-0">
                  {item.icon}
                </div>
                <h3 className="text-xs sm:text-sm font-extrabold text-slate-900 leading-snug">
                  {item.title}
                </h3>
              </div>
            ))}
          </div>
        </section>

        {/* ================================================================ */}
        {/* OUR STORY SECTION                                                */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="about-story-card bg-white rounded-3xl border border-slate-150 p-6 sm:p-10 lg:p-12 shadow-2xs">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
              {/* Left Image (Exact reference asset) */}
              <div className="lg:col-span-5 rounded-2xl overflow-hidden bg-slate-50 border border-slate-100 shadow-sm flex items-center justify-center">
                <img
                  src={aboutStory}
                  alt="Historic and modern city skyline"
                  className="w-full h-auto object-cover"
                />
              </div>

              {/* Right Content */}
              <div className="lg:col-span-7 space-y-4 text-left">
                <span className="inline-flex items-center px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-emerald-50 text-emerald-800 border border-emerald-200/60">
                  OUR STORY
                </span>

                <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight leading-tight">
                  A Cleaner, Safer, More Accountable Tomorrow
                </h2>

                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  CivicSense was founded with a simple belief: Every citizen deserves to live in a clean, safe and well-maintained city.
                </p>

                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  We saw civic issues around us — potholes, garbage, broken street lights, waterlogging — and realized that technology and community action can help solve them. CivicSense bridges the gap between citizens and municipal authorities, making it easier to report issues, track progress and create real change.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================ */}
        {/* MISSION / VISION / VALUES                                        */}
        {/* ================================================================ */}
        <section id="mission-section" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-4 sm:space-y-5">
          {/* Top Wide Mission Card */}
          <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-5 sm:p-8 shadow-2xs flex flex-col sm:flex-row items-start sm:items-center gap-4 sm:gap-5 text-left">
            <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-emerald-50 text-emerald-700 border border-emerald-100 flex items-center justify-center shrink-0">
              <TargetIcon className="w-6 h-6 sm:w-7 sm:h-7" />
            </div>
            <div className="space-y-1">
              <h3 className="text-base sm:text-lg font-extrabold text-slate-900 tracking-tight">
                Our Mission
              </h3>
              <p className="text-xs sm:text-sm text-slate-600 leading-relaxed italic font-medium">
                &ldquo;To empower every citizen to build cleaner, safer and happier cities through technology, transparency and community participation.&rdquo;
              </p>
            </div>
          </div>

          {/* Bottom Two Cards (Vision & Values) */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-5">
            {/* Our Vision */}
            <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-5 sm:p-8 shadow-2xs flex items-start gap-4 text-left">
              <div className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl bg-blue-50 text-blue-600 border border-blue-100 flex items-center justify-center shrink-0">
                <EyeIcon className="w-5 h-5 sm:w-6 sm:h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base sm:text-lg font-extrabold text-slate-900 tracking-tight">
                  Our Vision
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  An India where every city is clean, safe, well-maintained and responsive to its citizens.
                </p>
              </div>
            </div>

            {/* Our Values */}
            <div className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-5 sm:p-8 shadow-2xs flex items-start gap-4 text-left">
              <div className="w-11 h-11 sm:w-12 sm:h-12 rounded-2xl bg-red-50 text-red-500 border border-red-100 flex items-center justify-center shrink-0">
                <HeartIcon className="w-5 h-5 sm:w-6 sm:h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base sm:text-lg font-extrabold text-slate-900 tracking-tight">
                  Our Values
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
                  Transparency, community, technology, accountability and a cleaner, greener future for all.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* ================================================================ */}
        {/* THE PEOPLE BEHIND CIVICSENSE                                     */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-6 sm:mb-12 space-y-2">
            <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              The People Behind CivicSense
            </h2>
            <p className="text-xs sm:text-sm text-slate-500 font-medium">
              A passionate team committed to creating real change.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6">
            {teamValues.map((item) => (
              <div
                key={item.title}
                className="bg-white rounded-2xl sm:rounded-3xl border border-slate-150 p-5 sm:p-8 shadow-2xs hover:shadow-md transition-all duration-200 flex flex-col items-center text-center space-y-3"
              >
                <div
                  className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl border flex items-center justify-center shrink-0 ${item.iconBg}`}
                >
                  {item.icon}
                </div>
                <h3 className="text-base font-extrabold text-slate-900 tracking-tight">
                  {item.title}
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed font-medium">
                  {item.desc}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* ================================================================ */}
        {/* BOTTOM GREEN BANNER CTA                                          */}
        {/* ================================================================ */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-emerald-800 rounded-3xl p-6 sm:p-8 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
            <div className="flex items-center gap-3.5 text-left">
              <div className="w-10 h-10 rounded-2xl bg-emerald-700/80 border border-emerald-600 flex items-center justify-center shrink-0">
                <LeafLogo className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base sm:text-lg font-extrabold text-white tracking-tight">
                  Be a Part of the Change
                </h3>
                <p className="text-xs sm:text-sm text-emerald-100 font-normal">
                  Report issues, support your community, and help build better cities.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => navigate('/report-issue')}
              className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-white hover:bg-emerald-50 text-emerald-900 font-extrabold text-xs sm:text-sm shadow-md transition-all active:scale-95 cursor-pointer shrink-0 touch-target"
            >
              <span>Report an Issue Now</span>
              <ArrowRightIcon className="w-4 h-4 text-emerald-900" />
            </button>
          </div>
        </section>
      </main>

      {/* Footer */}
      <HomeFooter />
    </div>
  )
}
