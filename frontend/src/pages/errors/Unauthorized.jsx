import { Link } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'

export default function Unauthorized() {
  return (
    <AuthLayout eyebrow="401" title="Sign in required">
      <div className="text-center">
        <p className="text-sm text-slate-600">
          You need to log in to access this page.
        </p>
        <Link to="/login" className="btn-primary mt-6 inline-flex">
          Go to login
        </Link>
      </div>
    </AuthLayout>
  )
}
