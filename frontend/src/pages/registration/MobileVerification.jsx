import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import FormField from '../../components/common/FormField.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { sendOtp } from '../../api/otpApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import { validateMobileNumber } from '../../utils/validators.js'

export default function MobileVerification() {
  const navigate = useNavigate()
  const { data, updateData } = useRegistration()

  const [mobileNumber, setMobileNumber] = useState(data.mobileNumber)
  const [error, setError] = useState('')
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (!data.verificationId) {
      navigate('/register/identity', { replace: true })
    }
  }, [data.verificationId, navigate])

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')

    const validationError = validateMobileNumber(mobileNumber)
    setError(validationError)
    if (validationError) return

    setIsSubmitting(true)

    try {
      const result = await sendOtp({
        identityVerificationId: data.verificationId,
        mobileNumber,
      })

      updateData({
        mobileNumber,
        otpVerificationId: result.otpVerificationId,
      })

      navigate('/register/verify')
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 3 of 5"
      title="Verify your mobile number"
      subtitle="We'll send a one-time code to confirm it's you"
    >
      <StepIndicator currentStep="mobile" />

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
          error={error}
          disabled={isSubmitting}
        />

        <button
          type="submit"
          className="btn-primary w-full"
          disabled={isSubmitting}
        >
          {isSubmitting && <Spinner size={16} />}
          {isSubmitting ? 'Sending code…' : 'Send verification code'}
        </button>
      </form>
    </AuthLayout>
  )
}