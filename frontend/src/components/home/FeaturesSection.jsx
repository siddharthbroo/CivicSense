import { useEffect, useRef } from 'react'
import {
  ShieldCheckIcon,
  LeafIcon,
  UsersGroupIcon,
  TrackChartIcon,
} from './HomeIcons.jsx'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function FeaturesSection() {
  const sectionRef = useRef(null)

  useEffect(() => {
    if (prefersReducedMotion()) return

    const ctx = gsap.context(() => {
      gsap.from('.feature-card', {
        opacity: 0,
        y: 20,
        stagger: 0.1,
        duration: 0.55,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 88%',
          once: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  const features = [
    {
      title: 'Safer Neighborhoods',
      description: 'Report hazards and keep your community safe.',
      icon: ShieldCheckIcon,
      bg: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: 'Cleaner Cities',
      description: 'Help maintain clean and healthy surroundings.',
      icon: LeafIcon,
      bg: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: 'Stronger Communities',
      description: 'Support issues that matter to you.',
      icon: UsersGroupIcon,
      bg: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: 'Real Impact',
      description: 'Track progress and see change happen.',
      icon: TrackChartIcon,
      bg: 'bg-emerald-50 text-emerald-700',
    },
  ]

  return (
    <section
      ref={sectionRef}
      id="features"
      className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14"
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
        {features.map((feature) => {
          const Icon = feature.icon
          return (
            <div
              key={feature.title}
              className="feature-card bg-emerald-50/50 hover:bg-emerald-50/80 rounded-2xl p-4 sm:p-5 border border-emerald-100/70 transition-all duration-200 hover:-translate-y-1 hover:shadow-md flex items-start gap-3.5 sm:gap-4"
            >
              <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-2xl bg-white text-emerald-700 shadow-xs flex items-center justify-center shrink-0 border border-emerald-100">
                <Icon className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-snug">
                  {feature.title}
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 mt-1 leading-relaxed">
                  {feature.description}
                </p>
              </div>
            </div>
          )
        })}
      </div>
    </section>
  )
}
