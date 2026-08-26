import { Link } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'

export default function Forbidden() {
  return (
    <AuthLayout eyebrow="403" title="Access denied">
      <div className="text-center">
        <p className="text-sm text-slate-600">
          Your account does not have permission to view this page.
        </p>
        <Link to="/dashboard" className="btn-primary mt-6 inline-flex">
          Back to dashboard
        </Link>
      </div>
    </AuthLayout>
  )
}
