import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../../components/layout/AuthLayout.jsx'
import StepIndicator from '../../components/registration/StepIndicator.jsx'
import DocumentTypeSelect from '../../components/identity/DocumentTypeSelect.jsx'
import DocumentDropzone from '../../components/identity/DocumentDropzone.jsx'
import Alert from '../../components/common/Alert.jsx'
import Spinner from '../../components/common/Spinner.jsx'
import { uploadIdentityDocument } from '../../api/identityApi.js'
import { useRegistration } from '../../context/RegistrationContext.jsx'

export default function IdentityUpload() {
  const navigate = useNavigate()
  const { updateData } = useRegistration()

  const [documentType, setDocumentType] = useState('')
  const [file, setFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [serverError, setServerError] = useState('')
  const [isUploading, setIsUploading] = useState(false)

  function handleFileSelect(selectedFile, error) {
    setFile(selectedFile)
    setFileError(error || '')
  }

  function validate() {
    let valid = true
    if (!documentType) valid = false
    if (!file) {
      setFileError('Please select a document to upload.')
      valid = false
    }
    return valid
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setIsUploading(true)
    try {
      const result = await uploadIdentityDocument(file, documentType)
      updateData({
        verificationId: result.verificationId,
        documentType: result.documentType,
        name: result.extractedName || '',
        dateOfBirth: result.extractedDob || '',
        gender: result.extractedGender || '',
      })
      navigate('/register/review')
    } catch (err) {
      setServerError(err.message)
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Step 1 of 5"
      title="Verify your identity"
      subtitle="Upload a government-issued ID so we can pre-fill your details"
      width="max-w-lg"
    >
      <StepIndicator currentStep="identity" />

      <form onSubmit={handleSubmit} noValidate className="space-y-5">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        <DocumentTypeSelect value={documentType} onChange={setDocumentType} disabled={isUploading} />

        <DocumentDropzone
          file={file}
          onFileSelect={handleFileSelect}
          error={fileError}
          disabled={isUploading}
        />

        <Alert variant="info">
          Your document is processed securely by our backend to extract your name, date of
          birth, and gender. You will be able to review and correct this information next.
        </Alert>

        <button type="submit" className="btn-primary w-full" disabled={isUploading}>
          {isUploading && <Spinner size={16} />}
          {isUploading ? 'Processing document…' : 'Upload & continue'}
        </button>
      </form>
    </AuthLayout>
  )
}
