import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import FormField from '../../components/common/FormField.jsx'
import PasswordField from '../../components/common/PasswordField.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { sendOtp } from '../../api/otpApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import {
  validateName,
  validateEmail,
  validateMobileNumber,
  validatePasswordPair,
} from '../../utils/validators.js'

export default function AccountDetails() {
  const navigate = useNavigate()
  const { data, updateData } = useRegistration()

  const [name, setName] = useState(data.name || '')
  const [email, setEmail] = useState(data.email || '')
  const [mobileNumber, setMobileNumber] = useState(data.mobileNumber || '')
  const [password, setPassword] = useState(data.password || '')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  function validate() {
    const nameError = validateName(name)
    const emailError = validateEmail(email)
    const mobileError = validateMobileNumber(mobileNumber)
    const passwordErrors = validatePasswordPair(password, confirmPassword)

    const errors = {
      name: nameError,
      email: emailError,
      mobileNumber: mobileError,
      password: passwordErrors.password,
      confirmPassword: passwordErrors.confirmPassword,
    }

    setFieldErrors(errors)

    return (
      !nameError &&
      !emailError &&
      !mobileError &&
      !passwordErrors.password &&
      !passwordErrors.confirmPassword
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')

    if (!validate()) return

    setIsSubmitting(true)

    try {
      const result = await sendOtp({ mobileNumber: mobileNumber.trim() })

      updateData({
        name: name.trim(),
        email: email.trim(),
        mobileNumber: mobileNumber.trim(),
        password,
        otpVerificationId: result.otpVerificationId,
      })

      navigate('/register/verify')
    } catch (err) {
      setServerError(err.message || 'Failed to send verification code. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 1 of 3"
      title="Create your account"
      subtitle="Join CivicSense to report and track civic issues in your area"
    >
      <StepIndicator currentStep="details" />

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        <FormField
          id="name"
          label="Full name"
          required
          type="text"
          placeholder="e.g. Rajesh Kumar"
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={fieldErrors.name}
          disabled={isSubmitting}
        />

        <FormField
          id="email"
          label="Email address"
          required
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={fieldErrors.email}
          disabled={isSubmitting}
        />

        <FormField
          id="mobileNumber"
          label="Mobile number"
          required
          type="tel"
          inputMode="numeric"
          placeholder="10-digit mobile number"
          value={mobileNumber}
          onChange={(e) => setMobileNumber(e.target.value)}
          error={fieldErrors.mobileNumber}
          disabled={isSubmitting}
        />

        <PasswordField
          id="password"
          label="Password"
          required
          placeholder="At least 8 characters"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={fieldErrors.password}
          disabled={isSubmitting}
        />

        <PasswordField
          id="confirmPassword"
          label="Confirm password"
          required
          placeholder="Re-enter your password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          error={fieldErrors.confirmPassword}
          disabled={isSubmitting}
        />

        <button
          type="submit"
          className="btn-primary w-full mt-2 min-h-[44px] touch-target"
          disabled={isSubmitting}
        >
          {isSubmitting && <Spinner size={16} />}
          {isSubmitting ? 'Sending verification code…' : 'Continue to verification'}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-teal-700 hover:text-teal-800">
          Log in
        </Link>
      </p>
    </AuthLayout>
  )
}