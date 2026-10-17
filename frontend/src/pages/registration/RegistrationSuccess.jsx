import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import { gsap, prefersReducedMotion } from '../../utils/motion.js'

export default function RegistrationSuccess() {
  const navigate = useNavigate()
  const { data } = useRegistration()
  const containerRef = useRef(null)

  // Guard against reaching this screen without completing registration
  useEffect(() => {
    if (!data.registeredUser) {
      navigate('/register', { replace: true })
    }
  }, [data.registeredUser, navigate])

  useEffect(() => {
    if (prefersReducedMotion() || !data.registeredUser) return

    const ctx = gsap.context(() => {
      gsap.from('.success-badge', {
        scale: 0.5,
        opacity: 0,
        duration: 0.5,
        ease: 'back.out(1.5)',
      })
    }, containerRef)

    return () => ctx.revert()
  }, [data.registeredUser])

  if (!data.registeredUser) return null

  return (
    <AuthLayout
      eyebrow="Step 3 of 3 · Account created"
      title="Welcome to CivicSense"
      subtitle="Your citizen account is ready"
    >
      <StepIndicator currentStep="complete" />

      <div ref={containerRef} className="flex flex-col items-center text-center">
        <span className="success-badge flex h-14 w-14 items-center justify-center rounded-full bg-teal-50 text-teal-600 ring-1 ring-inset ring-teal-200">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M5 13l4 4L19 7"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>

        <p className="mt-4 text-sm text-slate-600 leading-relaxed">
          Your account has been created successfully,{' '}
          <strong className="text-navy-900">{data.registeredUser.name}</strong>.
          You can now log in to report civic issues, track complaint resolution,
          and engage with your local community.
        </p>

        <dl className="mt-6 w-full space-y-2.5 rounded-xl bg-slate-50 border border-slate-200 p-4 text-left text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Citizen Name</dt>
            <dd className="font-medium text-navy-800">{data.registeredUser.name}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Mobile number</dt>
            <dd className="font-medium text-navy-800">{data.registeredUser.mobileNumber}</dd>
          </div>
          {data.email && (
            <div className="flex justify-between">
              <dt className="text-slate-500">Email address</dt>
              <dd className="font-medium text-navy-800">{data.email}</dd>
            </div>
          )}
          <div className="flex justify-between">
            <dt className="text-slate-500">Account Role</dt>
            <dd className="font-medium text-teal-700 font-semibold">{data.registeredUser.role}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Account Status</dt>
            <dd className="inline-flex items-center gap-1 font-medium text-emerald-700">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
              {data.registeredUser.status}
            </dd>
          </div>
        </dl>

        <button
          onClick={() => navigate('/login')}
          className="btn-primary mt-6 w-full min-h-[44px] touch-target"
        >
          Continue to login
        </button>
      </div>
    </AuthLayout>
  )
}