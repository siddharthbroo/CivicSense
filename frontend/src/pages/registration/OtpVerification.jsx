import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import OtpInput from '../../components/registration/OtpInput.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { verifyOtp, resendOtp } from '../../api/otpApi.js'
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

  useEffect(() => {
    if (!data.mobileNumber || !data.otpVerificationId) {
      navigate('/register/mobile', { replace: true })
    }
  }, [data.mobileNumber, data.otpVerificationId, navigate])

  async function handleVerify(e) {
    e.preventDefault()
    setServerError('')

    if (code.length !== 6) {
      setError('Enter the full 6-digit code.')
      return
    }

    setError('')
    setIsVerifying(true)

    try {
      const result = await verifyOtp({
        otpVerificationId: data.otpVerificationId,
        otp: code,
      })

      if (!result.verified) {
        setServerError(result.message || 'Invalid OTP')
        return
      }

      navigate('/register/password')

    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsVerifying(false)
    }
  }

  async function handleResend() {
    setServerError('')
    setIsResending(true)

    try {
      const result = await resendOtp({
        identityVerificationId: data.verificationId,
        mobileNumber: data.mobileNumber,
      })

      updateData({
        otpVerificationId: result.otpVerificationId,
      })

      restart()
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsResending(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 4 of 5"
      title="Enter verification code"
      subtitle={`Sent to your mobile number ending in ${data.mobileNumber?.slice(-4) || '••••'}`}
    >
      <StepIndicator currentStep="otp" />

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
          className="btn-primary w-full"
          disabled={isVerifying}
        >
          {isVerifying && <Spinner size={16} />}
          {isVerifying ? 'Verifying…' : 'Verify code'}
        </button>
      </form>
    </AuthLayout>
  )
}