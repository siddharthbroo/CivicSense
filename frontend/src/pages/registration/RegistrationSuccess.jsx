import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import { useRegistration } from '../../context/RegistrationContext.jsx'

export default function RegistrationSuccess() {
  const navigate = useNavigate()
  const { data } = useRegistration()

  // Guard against reaching this screen without completing registration.
  useEffect(() => {
    if (!data.registeredUser) {
      navigate('/register/identity', { replace: true })
    }
  }, [data.registeredUser, navigate])

  if (!data.registeredUser) return null

  return (
    <AuthLayout eyebrow="Registration complete" title="Welcome to CivicSense">
      <div className="flex flex-col items-center text-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-full bg-green-50 text-green-600 ring-1 ring-inset ring-green-200">
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

        <p className="mt-4 text-sm text-slate-600">
          Your account has been created successfully, {data.registeredUser.name}. You can now
          log in to report and track civic complaints.
        </p>

        <dl className="mt-6 w-full space-y-2 rounded-md bg-slate-25 p-4 text-left text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Mobile number</dt>
            <dd className="font-medium text-navy-800">{data.registeredUser.mobileNumber}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Role</dt>
            <dd className="font-medium text-navy-800">{data.registeredUser.role}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Status</dt>
            <dd className="font-medium text-navy-800">{data.registeredUser.status}</dd>
          </div>
        </dl>

        <button
          onClick={() => navigate('/login')}
          className="btn-accent mt-6 w-full"
        >
          Continue to login
        </button>
      </div>
    </AuthLayout>
  )
}
