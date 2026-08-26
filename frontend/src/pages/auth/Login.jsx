import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import FormField from '../../components/common/FormField.jsx'
import PasswordField from '../../components/common/PasswordField.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { login as loginRequest } from '../../api/authApi.js'
import { useAuth } from '../../context/AuthContext.jsx'
import { validateMobileNumber, isRequired } from '../../utils/validators.js'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [mobileNumber, setMobileNumber] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  function validate() {
    const errors = {
      mobileNumber: validateMobileNumber(mobileNumber),
      password: isRequired(password) ? '' : 'Password is required.',
    }
    setFieldErrors(errors)
    return !errors.mobileNumber && !errors.password
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setIsSubmitting(true)
    try {
      const response = await loginRequest({ mobileNumber, password })
      login(response)
      const redirectTo = location.state?.from?.pathname || '/dashboard'
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Citizen · Officer · Admin"
      title="Log in to CivicSense"
      subtitle="Access your civic complaint dashboard"
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        <FormField
          id="mobileNumber"
          label="Mobile number"
          required
          type="tel"
          inputMode="numeric"
          placeholder="9876543210"
          value={mobileNumber}
          onChange={(e) => setMobileNumber(e.target.value)}
          error={fieldErrors.mobileNumber}
          disabled={isSubmitting}
        />

        <PasswordField
          id="password"
          label="Password"
          required
          placeholder="Enter your password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          disabled={isSubmitting}
        />

        <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting && <Spinner size={16} />}
          {isSubmitting ? 'Logging in…' : 'Log in'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        New to CivicSense?{' '}
        <Link to="/register/identity" className="font-semibold text-teal-700 hover:text-teal-800">
          Create an account
        </Link>
      </p>
    </AuthLayout>
  )
}
