import { useState, useRef } from 'react'
import Alert from '../common/Alert.jsx'
import Spinner from '../common/Spinner.jsx'
import { createComplaint } from '../../api/complaintApi.js'

const QUICK_CATEGORIES = [
  { id: 'pothole', label: 'Road / Pothole', icon: '🛣️', template: 'Deep pothole / damaged road surface observed near' },
  { id: 'garbage', label: 'Garbage & Waste', icon: '🗑️', template: 'Uncollected garbage and waste accumulation near' },
  { id: 'streetlight', label: 'Street Light', icon: '💡', template: 'Street lights are non-functional or broken near' },
  { id: 'water', label: 'Water / Sewage', icon: '💧', template: 'Water leakage / open drain issue causing inconvenience near' },
  { id: 'sanitation', label: 'Sanitation', icon: '🧹', template: 'Unsanitary conditions and debris requiring municipal cleaning near' },
]

const MAX_IMAGE_SIZE_MB = 10
const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png']

export default function ComplaintForm({ onComplaintCreated }) {
  const fileInputRef = useRef(null)

  const [description, setDescription] = useState('')
  const [address, setAddress] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [locationStatus, setLocationStatus] = useState('idle') // idle | detecting | success | error
  const [locationError, setLocationError] = useState('')

  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [isDragging, setIsDragging] = useState(false)

  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Quick category chip handler
  function handleCategoryClick(template) {
    if (!description || description.trim() === '') {
      setDescription(template + ' ')
    } else {
      setDescription((prev) => prev + ' (' + template + ')')
    }
  }

  // Geolocation detection handler
  function handleDetectLocation() {
    setLocationError('')
    if (!navigator.geolocation) {
      setLocationStatus('error')
      setLocationError('Geolocation is not supported by your browser.')
      return
    }

    setLocationStatus('detecting')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = Number(position.coords.latitude.toFixed(6))
        const lng = Number(position.coords.longitude.toFixed(6))
        setLatitude(lat)
        setLongitude(lng)
        setLocationStatus('success')
        setFieldErrors((prev) => ({ ...prev, location: '' }))
      },
      (error) => {
        setLocationStatus('error')
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setLocationError('Location permission denied. Please allow GPS access or enter coordinates.')
            break
          case error.POSITION_UNAVAILABLE:
            setLocationError('Location information is unavailable. Please check your GPS signal.')
            break
          case error.TIMEOUT:
            setLocationError('Location request timed out. Please try again.')
            break
          default:
            setLocationError('Failed to retrieve location. Please enter manually.')
            break
        }
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  }

  // Image selection validation
  function handleImageSelect(file) {
    if (!file) return

    if (!ACCEPTED_IMAGE_TYPES.includes(file.type.toLowerCase())) {
      setFieldErrors((prev) => ({
        ...prev,
        image: 'Only JPG, JPEG, or PNG images are supported.',
      }))
      return
    }

    if (file.size > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
      setFieldErrors((prev) => ({
        ...prev,
        image: `Image size must not exceed ${MAX_IMAGE_SIZE_MB}MB.`,
      }))
      return
    }

    setFieldErrors((prev) => ({ ...prev, image: '' }))
    setImageFile(file)

    const previewUrl = URL.createObjectURL(file)
    setImagePreview(previewUrl)
  }

  function handleRemoveImage() {
    if (imagePreview) {
      URL.revokeObjectURL(imagePreview)
    }
    setImageFile(null)
    setImagePreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  // Form validation
  function validate() {
    const errors = {}

    if (!description || description.trim().length < 10) {
      errors.description = 'Description must be at least 10 characters long.'
    } else if (description.length > 5000) {
      errors.description = 'Description cannot exceed 5000 characters.'
    }

    if (latitude === '' || longitude === '') {
      errors.location = 'GPS Location is required. Click "Detect My Location" or enter coordinates.'
    } else {
      const latNum = parseFloat(latitude)
      const lngNum = parseFloat(longitude)
      if (isNaN(latNum) || latNum < -90 || latNum > 90) {
        errors.location = 'Please provide a valid latitude (-90 to 90).'
      }
      if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
        errors.location = 'Please provide a valid longitude (-180 to 180).'
      }
    }

    if (!imageFile) {
      errors.image = 'An issue photo is required to submit a complaint.'
    }

    if (address && address.length > 500) {
      errors.address = 'Address must not exceed 500 characters.'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')

    if (!validate()) return

    setIsSubmitting(true)
    try {
      const response = await createComplaint({
        description: description.trim(),
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        address: address.trim() || undefined,
        image: imageFile,
      })

      // Pass created complaint info to parent
      onComplaintCreated?.({
        id: response.complaintId,
        description: description.trim(),
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        address: address.trim(),
        status: response.status || 'SUBMITTED',
        createdAt: response.createdAt || new Date().toISOString(),
        imageUrl: imagePreview,
      })

      // Reset form
      setDescription('')
      setAddress('')
      setLatitude('')
      setLongitude('')
      setLocationStatus('idle')
      handleRemoveImage()
    } catch (err) {
      setServerError(err.message || 'Failed to submit complaint. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="card overflow-hidden p-6 sm:p-8">
      <div className="mb-6 border-b border-slate-200 pb-5">
        <h2 className="text-xl font-bold text-navy-900">File a New Civic Complaint</h2>
        <p className="mt-1 text-sm text-slate-500">
          Provide complete details and a clear photograph to help municipal officers locate and resolve the issue quickly.
        </p>
      </div>

      {serverError && <Alert variant="error" className="mb-6">{serverError}</Alert>}

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {/* Quick Issue Templates */}
        <div>
          <label className="field-label">Quick Issue Category</label>
          <div className="flex flex-wrap gap-2">
            {QUICK_CATEGORIES.map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => handleCategoryClick(cat.template)}
                disabled={isSubmitting}
                className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-teal-400 hover:bg-teal-50 hover:text-teal-900"
              >
                <span>{cat.icon}</span>
                <span>{cat.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Complaint Description */}
        <div>
          <div className="flex items-center justify-between">
            <label htmlFor="description" className="field-label">
              Complaint Description <span className="text-teal-700">*</span>
            </label>
            <span className={`text-xs ${description.length > 5000 ? 'text-red-600 font-semibold' : 'text-slate-400'}`}>
              {description.length} / 5000
            </span>
          </div>
          <textarea
            id="description"
            rows={4}
            className={`input-field resize-y ${fieldErrors.description ? 'input-field-error' : ''}`}
            placeholder="Describe the civic issue in detail (e.g., location specifics, hazard level, since when it has been occurring)..."
            value={description}
            onChange={(e) => {
              setDescription(e.target.value)
              if (fieldErrors.description) {
                setFieldErrors((prev) => ({ ...prev, description: '' }))
              }
            }}
            disabled={isSubmitting}
          />
          {fieldErrors.description && <p className="field-error">{fieldErrors.description}</p>}
          <p className="mt-1 text-xs text-slate-500">
            Minimum 10 characters. Can be written in English, Hindi, or Hinglish.
          </p>
        </div>

        {/* Image Upload */}
        <div>
          <label className="field-label">
            Issue Photograph (Proof) <span className="text-teal-700">*</span>
          </label>

          {imagePreview ? (
            <div className="relative flex flex-col items-center justify-center rounded-lg border-2 border-slate-200 bg-slate-50 p-4 sm:flex-row sm:justify-start sm:gap-6">
              <img
                src={imagePreview}
                alt="Selected issue"
                className="h-32 w-32 rounded-md object-cover shadow-sm ring-1 ring-slate-300"
              />
              <div className="mt-3 text-center sm:mt-0 sm:text-left">
                <p className="text-sm font-semibold text-navy-900">{imageFile?.name}</p>
                <p className="mt-0.5 text-xs text-slate-500">
                  {((imageFile?.size || 0) / (1024 * 1024)).toFixed(2)} MB · {imageFile?.type}
                </p>
                <button
                  type="button"
                  onClick={handleRemoveImage}
                  disabled={isSubmitting}
                  className="btn-secondary mt-3 px-3 py-1.5 text-xs text-red-600 hover:bg-red-50 hover:border-red-300"
                >
                  Remove & Replace
                </button>
              </div>
            </div>
          ) : (
            <div
              onDragOver={(e) => {
                e.preventDefault()
                if (!isSubmitting) setIsDragging(true)
              }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={(e) => {
                e.preventDefault()
                setIsDragging(false)
                if (!isSubmitting) handleImageSelect(e.dataTransfer.files?.[0])
              }}
              onClick={() => !isSubmitting && fileInputRef.current?.click()}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if ((e.key === 'Enter' || e.key === ' ') && !isSubmitting) fileInputRef.current?.click()
              }}
              className={`flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed px-6 py-7 text-center transition-colors ${
                isDragging
                  ? 'border-teal-500 bg-teal-50'
                  : fieldErrors.image
                  ? 'border-red-400 bg-red-50/30'
                  : 'border-slate-300 bg-slate-25 hover:border-teal-400 hover:bg-teal-50/20'
              } ${isSubmitting ? 'cursor-not-allowed opacity-60' : ''}`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/jpg,image/png"
                className="sr-only"
                disabled={isSubmitting}
                onChange={(e) => handleImageSelect(e.target.files?.[0])}
              />
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-teal-50 text-teal-600">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <p className="mt-3 text-sm font-medium text-navy-800">
                <span className="text-teal-700">Click to upload issue photo</span> or drag and drop
              </p>
              <p className="mt-1 text-xs text-slate-500">JPG, JPEG, or PNG up to {MAX_IMAGE_SIZE_MB}MB</p>
            </div>
          )}
          {fieldErrors.image && <p className="field-error">{fieldErrors.image}</p>}
        </div>

        {/* GPS Location & Landmark Address */}
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 sm:p-5">
          <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
            <div>
              <span className="text-sm font-bold text-navy-900">Issue Location</span>
              <p className="text-xs text-slate-500">Accurate GPS coordinates help officers dispatch teams to the exact spot.</p>
            </div>
            <button
              type="button"
              onClick={handleDetectLocation}
              disabled={isSubmitting || locationStatus === 'detecting'}
              className="btn-secondary inline-flex items-center gap-1.5 border-teal-600 bg-white px-3 py-1.5 text-xs font-semibold text-teal-700 hover:bg-teal-50 disabled:border-slate-300 disabled:text-slate-400"
            >
              {locationStatus === 'detecting' ? (
                <>
                  <Spinner size={14} />
                  <span>Detecting GPS…</span>
                </>
              ) : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M12 2v2m0 16v2M2 12h2m16 0h2m-4.93-7.07l-1.41 1.41m-9.32 9.32l-1.41 1.41m0-12.14l1.41 1.41m9.32 9.32l1.41 1.41M12 8a4 4 0 100 8 4 4 0 000-8z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <span>Detect My Location</span>
                </>
              )}
            </button>
          </div>

          {locationStatus === 'success' && (
            <div className="mt-3 flex items-center gap-2 rounded-md bg-teal-100/60 px-3 py-2 text-xs font-medium text-teal-900 ring-1 ring-inset ring-teal-300">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-teal-700" aria-hidden="true">
                <path d="M5 13l4 4L19 7" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <span>GPS Coordinates captured successfully!</span>
            </div>
          )}

          {locationError && (
            <p className="mt-2 text-xs font-medium text-red-600">{locationError}</p>
          )}

          <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="latitude" className="field-label text-xs">
                Latitude <span className="text-teal-700">*</span>
              </label>
              <input
                id="latitude"
                type="number"
                step="any"
                className={`input-field py-2 text-xs ${fieldErrors.location ? 'input-field-error' : ''}`}
                placeholder="e.g. 28.613939"
                value={latitude}
                onChange={(e) => {
                  setLatitude(e.target.value)
                  if (fieldErrors.location) setFieldErrors((prev) => ({ ...prev, location: '' }))
                }}
                disabled={isSubmitting}
              />
            </div>
            <div>
              <label htmlFor="longitude" className="field-label text-xs">
                Longitude <span className="text-teal-700">*</span>
              </label>
              <input
                id="longitude"
                type="number"
                step="any"
                className={`input-field py-2 text-xs ${fieldErrors.location ? 'input-field-error' : ''}`}
                placeholder="e.g. 77.209021"
                value={longitude}
                onChange={(e) => {
                  setLongitude(e.target.value)
                  if (fieldErrors.location) setFieldErrors((prev) => ({ ...prev, location: '' }))
                }}
                disabled={isSubmitting}
              />
            </div>
          </div>
          {fieldErrors.location && <p className="field-error">{fieldErrors.location}</p>}

          <div className="mt-4">
            <label htmlFor="address" className="field-label text-xs">
              Landmark / Detailed Address (Optional)
            </label>
            <input
              id="address"
              type="text"
              className={`input-field py-2 text-xs ${fieldErrors.address ? 'input-field-error' : ''}`}
              placeholder="e.g. Opposite Sector 14 Gate 2, near Central Bank ATM"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              disabled={isSubmitting}
              maxLength={500}
            />
            {fieldErrors.address && <p className="field-error">{fieldErrors.address}</p>}
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="submit"
            className="btn-accent w-full sm:w-auto sm:min-w-[180px]"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <Spinner size={16} />
                <span>Uploading & Filing…</span>
              </>
            ) : (
              <span>Submit Complaint</span>
            )}
          </button>
        </div>
      </form>
    </div>
  )
}

