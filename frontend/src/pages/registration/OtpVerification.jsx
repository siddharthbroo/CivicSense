import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import OtpInput from '../../components/registration/OtpInput.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { verifyOtp, resendOtp } from '../../api/otpApi.js'
import { registerUser } from '../../api/registrationApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import { useCountdown } from '../../hooks/useCountdown.js'

const RESEND_SECONDS = 30

export default function OtpVerification() {
  const navigate = useNavigate()
  const { data, updateData } = useRegistration()
  const { remaining, isFinished, restart } = useCountdown(RESEND_SECONDS)

  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [serverError, setServerError] = useState('')
  const [isVerifying, setIsVerifying] = useState(false)
  const [isResending, setIsResending] = useState(false)

  // Guard: if user navigates directly without completing account details
  useEffect(() => {
    if (!data.mobileNumber || !data.otpVerificationId || !data.password) {
      navigate('/register', { replace: true })
    }
  }, [data.mobileNumber, data.otpVerificationId, data.password, navigate])

  async function handleVerify(e) {
    e.preventDefault()
    setServerError('')

    if (code.length !== 6) {
      setError('Enter the full 6-digit verification code.')
      return
    }

    setError('')
    setIsVerifying(true)

    try {
      // 1. Verify OTP with backend
      const verifyResult = await verifyOtp({
        otpVerificationId: data.otpVerificationId,
        otp: code,
      })

      if (!verifyResult.verified) {
        setServerError(verifyResult.message || 'Invalid or expired OTP. Please try again.')
        return
      }

      // 2. Create the citizen account
      const userResult = await registerUser({
        otpVerificationId: data.otpVerificationId,
        name: data.name,
        email: data.email,
        password: data.password,
      })

      updateData({ registeredUser: userResult })
      navigate('/register/success')
    } catch (err) {
      setServerError(err.message || 'Verification or account creation failed. Please try again.')
    } finally {
      setIsVerifying(false)
    }
  }

  async function handleResend() {
    setServerError('')
    setIsResending(true)

    try {
      const result = await resendOtp({
        mobileNumber: data.mobileNumber,
      })

      updateData({
        otpVerificationId: result.otpVerificationId,
      })

      restart()
    } catch (err) {
      setServerError(err.message || 'Failed to resend code. Please try again.')
    } finally {
      setIsResending(false)
    }
  }

  const maskedMobile = data.mobileNumber
    ? `••••••${data.mobileNumber.slice(-4)}`
    : 'your mobile'

  return (
    <AuthLayout
      eyebrow="Step 2 of 3"
      title="Verify your mobile number"
      subtitle={`Enter the 6-digit code sent to ${maskedMobile}`}
    >
      <StepIndicator currentStep="verify" />

      <form onSubmit={handleVerify} noValidate className="space-y-5">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        <OtpInput
          value={code}
          onChange={setCode}
          error={error}
          disabled={isVerifying}
        />

        <div className="flex items-center justify-between text-sm">
          <span className="text-slate-500">
            {isFinished
              ? "Didn't receive a code?"
              : `Resend available in ${remaining}s`}
          </span>

          <button
            type="button"
            onClick={handleResend}
            disabled={!isFinished || isResending}
            className="font-semibold text-teal-700 hover:text-teal-800 disabled:cursor-not-allowed disabled:text-slate-300"
          >
            {isResending ? 'Resending…' : 'Resend code'}
          </button>
        </div>

        <button
          type="submit"
          className="btn-primary w-full min-h-[44px] touch-target"
          disabled={isVerifying}
        >
          {isVerifying && <Spinner size={16} />}
          {isVerifying ? 'Verifying & creating account…' : 'Verify & Create Account'}
        </button>
      </form>

      <div className="mt-6 flex items-center justify-between text-sm text-slate-500">
        <button
          type="button"
          onClick={() => navigate('/register')}
          className="font-medium text-slate-600 hover:text-navy-800"
        >
          ← Edit details
        </button>

        <Link to="/login" className="font-semibold text-teal-700 hover:text-teal-800">
          Already registered? Log in
        </Link>
      </div>
    </AuthLayout>
  )
}