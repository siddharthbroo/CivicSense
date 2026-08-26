import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import FormField from '../../components/common/FormField.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { confirmIdentity } from '../../api/identityApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'
import { isRequired } from '../../utils/validators.js'

const GENDER_OPTIONS = ['MALE', 'FEMALE', 'OTHER']

export default function IdentityReview() {
  const navigate = useNavigate()
  const { data, updateData } = useRegistration()

  const [name, setName] = useState(data.name)
  const [dateOfBirth, setDateOfBirth] = useState(data.dateOfBirth)
  const [gender, setGender] = useState(data.gender)
  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // If the user lands here directly without completing step 1, send
  // them back — there is no verificationId to confirm.
  useEffect(() => {
    if (!data.verificationId) {
      navigate('/register/identity', { replace: true })
    }
  }, [data.verificationId, navigate])

  function validate() {
    const errors = {
      name: isRequired(name) ? '' : 'Name is required.',
      dateOfBirth: isRequired(dateOfBirth) ? '' : 'Date of birth is required.',
      gender: isRequired(gender) ? '' : 'Gender is required.',
    }
    setFieldErrors(errors)
    return Object.values(errors).every((e) => !e)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setIsSubmitting(true)
    try {
      await confirmIdentity({
        verificationId: data.verificationId,
        name,
        dateOfBirth,
        gender,
      })
      updateData({ name, dateOfBirth, gender })
      navigate('/register/mobile')
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 2 of 5"
      title="Confirm your details"
      subtitle="We pre-filled this from your document — please review before continuing"
      width="max-w-lg"
    >
      <StepIndicator currentStep="review" />

      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        <FormField
          id="name"
          label="Full name"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={fieldErrors.name}
          disabled={isSubmitting}
        />

        <FormField
          id="dateOfBirth"
          label="Date of birth"
          required
          type="date"
          value={dateOfBirth}
          onChange={(e) => setDateOfBirth(e.target.value)}
          error={fieldErrors.dateOfBirth}
          disabled={isSubmitting}
        />

        <div>
          <label htmlFor="gender" className="field-label">
            Gender
            <span className="ml-0.5 text-teal-700">*</span>
          </label>
          <select
            id="gender"
            className={`input-field ${fieldErrors.gender ? 'input-field-error' : ''}`}
            value={gender}
            disabled={isSubmitting}
            onChange={(e) => setGender(e.target.value)}
          >
            <option value="" disabled>
              Select gender
            </option>
            {GENDER_OPTIONS.map((g) => (
              <option key={g} value={g}>
                {g.charAt(0) + g.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
          {fieldErrors.gender && <p className="field-error">{fieldErrors.gender}</p>}
        </div>

        <button type="submit" className="btn-primary w-full" disabled={isSubmitting}>
          {isSubmitting && <Spinner size={16} />}
          {isSubmitting ? 'Confirming…' : 'Confirm & continue'}
        </button>
      </form>
    </AuthLayout>
  )
}
