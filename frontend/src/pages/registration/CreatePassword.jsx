import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import PasswordField from '../../components/common/PasswordField.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { registerUser } from '../../api/registrationApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import { validatePasswordPair } from '../../utils/validators.js'

export default function CreatePassword() {
  const navigate = useNavigate()
  const { data, updateData } = useRegistration()

  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (!data.mobileNumber) {
      navigate('/register/mobile', { replace: true })
    }
  }, [data.mobileNumber, navigate])

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')

    const errors = validatePasswordPair(password, confirmPassword)
    setFieldErrors(errors)
    if (errors.password || errors.confirmPassword) return

    if (!data.otpVerificationId) {
      setServerError(
        'Your verified session reference is missing. Please verify your mobile number again.',
      )
      return
    }

    setIsSubmitting(true)
    try {
      const result = await registerUser({
        otpVerificationId: data.otpVerificationId,
        password,
      })
      updateData({ registeredUser: result })
      navigate('/register/success')
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 5 of 5"
      title="Create a password"
      subtitle="Choose a strong password to secure your CivicSense account"
    >
      <StepIndicator currentStep="password" />

      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {serverError && <Alert variant="error">{serverError}</Alert>}

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

        <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting && <Spinner size={16} />}
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthLayout>
  )
}
