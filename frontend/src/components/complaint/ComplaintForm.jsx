import { useState, useRef } from 'react'
import Alert from '../common/Alert.jsx'
import Spinner from '../common/Spinner.jsx'
import { createComplaint } from '../../api/complaintApi.js'

const CIVIC_CATEGORIES = [
  {
    id: 'pothole',
    label: 'Road & Pothole',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M4 19L8 5h8l4 14" />
        <path d="M12 5v4m0 4v2m0 4v1" />
        <circle cx="10" cy="14" r="2" />
      </svg>
    ),
    color: 'text-amber-700 bg-amber-50 border-amber-200',
    template: 'Damaged road / pothole observed near',
  },
  {
    id: 'garbage',
    label: 'Garbage & Waste',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M3 6h18" />
        <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
        <line x1="10" y1="11" x2="10" y2="17" />
        <line x1="14" y1="11" x2="14" y2="17" />
      </svg>
    ),
    color: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    template: 'Uncollected garbage and waste accumulation near',
  },
  {
    id: 'streetlight',
    label: 'Street Light',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 2a7 7 0 00-7 7c0 2.38 1.19 4.47 3 5.74V17a2 2 0 002 2h4a2 2 0 002-2v-2.26c1.81-1.27 3-3.36 3-5.74a7 7 0 00-7-7z" />
        <path d="M9 21h6" />
      </svg>
    ),
    color: 'text-yellow-700 bg-yellow-50 border-yellow-200',
    template: 'Street lights are broken or non-functional near',
  },
  {
    id: 'water',
    label: 'Water & Sewage',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 2.69l5.66 5.66a8 8 0 11-11.31 0z" />
      </svg>
    ),
    color: 'text-blue-700 bg-blue-50 border-blue-200',
    template: 'Water pipeline leakage / open sewer overflowing near',
  },
  {
    id: 'sanitation',
    label: 'Sanitation & Drains',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      </svg>
    ),
    color: 'text-teal-700 bg-teal-50 border-teal-200',
    template: 'Choked drain and unsanitary conditions near',
  },
  {
    id: 'other',
    label: 'Other Civic Issue',
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
    ),
    color: 'text-purple-700 bg-purple-50 border-purple-200',
    template: 'Civic issue requiring municipal attention near',
  },
]

const MAX_IMAGE_SIZE_MB = 10
const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png']

export default function ComplaintForm({ onComplaintCreated }) {
  const fileInputRef = useRef(null)

  const [selectedCategory, setSelectedCategory] = useState(null)
  const [description, setDescription] = useState('')
  const [address, setAddress] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [showManualCoords, setShowManualCoords] = useState(false)
  const [locationStatus, setLocationStatus] = useState('idle') // idle | detecting | success | error
  const [locationError, setLocationError] = useState('')

  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [isDragging, setIsDragging] = useState(false)

  const [fieldErrors, setFieldErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Category selection handler
  function handleCategorySelect(cat) {
    setSelectedCategory(cat.id)
    if (!description || description.trim() === '') {
      setDescription(cat.template + ' ')
    }
  }

  // Geolocation detection handler
  function handleDetectLocation() {
    setLocationError('')
    if (!navigator.geolocation) {
      setLocationStatus('error')
      setLocationError('Geolocation is not supported by your browser.')
      setShowManualCoords(true)
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
        setShowManualCoords(true)
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setLocationError('Location permission denied. Please enable GPS or enter coordinates manually.')
            break
          case error.POSITION_UNAVAILABLE:
            setLocationError('Location signal unavailable. Please enter coordinates manually.')
            break
          case error.TIMEOUT:
            setLocationError('Location request timed out. Please try again or enter manually.')
            break
          default:
            setLocationError('Failed to retrieve location. Please enter manually.')
            break
        }
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
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
      errors.description = 'Please describe the issue in at least 10 characters.'
    } else if (description.length > 5000) {
      errors.description = 'Description cannot exceed 5000 characters.'
    }

    if (latitude === '' || longitude === '') {
      errors.location = 'GPS Location is required. Tap "Detect My Location" or enter coordinates below.'
    } else {
      const latNum = parseFloat(latitude)
      const lngNum = parseFloat(longitude)
      if (isNaN(latNum) || latNum < -90 || latNum > 90) {
        errors.location = 'Please enter a valid latitude between -90 and 90.'
      }
      if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
        errors.location = 'Please enter a valid longitude between -180 and 180.'
      }
    }

    if (!imageFile) {
      errors.image = 'An issue photograph is required as physical proof.'
    }

    if (address && address.length > 500) {
      errors.address = 'Landmark address must not exceed 500 characters.'
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

      // Notify parent
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
      setSelectedCategory(null)
      setLocationStatus('idle')
      handleRemoveImage()
    } catch (err) {
      setServerError(err.message || 'Failed to submit complaint. Please check your connection and try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="card overflow-hidden shadow-card border border-slate-200">
      {/* Header Banner */}
      <div className="border-b border-slate-100 bg-slate-50/75 p-5 sm:p-6">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-teal-600 text-white shadow-sm">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z" />
            </svg>
          </div>
          <div>
            <h2 className="font-display text-lg sm:text-xl font-bold text-navy-900">
              Report a Civic Issue
            </h2>
            <p className="text-xs text-slate-500">
              Directly notify your municipal authority with photo evidence and location.
            </p>
          </div>
        </div>
      </div>

      {/* Form Content */}
      <form onSubmit={handleSubmit} noValidate className="p-5 sm:p-7 space-y-6">
        {serverError && <Alert variant="error">{serverError}</Alert>}

        {/* 1. Category Selection */}
        <div>
          <label className="field-label flex items-center justify-between">
            <span>
              1. Select Issue Category <span className="text-teal-700">*</span>
            </span>
            <span className="text-[11px] text-slate-400">Tap to auto-fill</span>
          </label>

          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            {CIVIC_CATEGORIES.map((cat) => {
              const isSelected = selectedCategory === cat.id

              return (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => handleCategorySelect(cat)}
                  disabled={isSubmitting}
                  className={`flex items-center gap-2.5 rounded-xl border p-3 text-left transition-all duration-150 active:scale-[0.98] ${
                    isSelected
                      ? 'border-teal-600 bg-teal-50/60 ring-2 ring-teal-500 text-teal-950 font-semibold shadow-sm'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/60 text-slate-700'
                  }`}
                >
                  <div className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${cat.color}`}>
                    {cat.icon}
                  </div>
                  <span className="text-xs font-medium leading-tight">
                    {cat.label}
                  </span>
                </button>
              )
            })}
          </div>
        </div>

        {/* 2. Photo Upload Dropzone */}
        <div>
          <label className="field-label flex items-center justify-between">
            <span>
              2. Upload Photo Proof <span className="text-teal-700">*</span>
            </span>
            <span className="text-[11px] text-slate-400">JPG, PNG up to {MAX_IMAGE_SIZE_MB}MB</span>
          </label>

          {imagePreview ? (
            <div className="relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col sm:flex-row items-center gap-4">
                <div className="relative h-28 w-28 shrink-0 overflow-hidden rounded-lg border border-slate-200 shadow-sm bg-white">
                  <img
                    src={imagePreview}
                    alt="Uploaded issue"
                    className="h-full w-full object-cover"
                  />
                </div>
                <div className="flex-1 text-center sm:text-left">
                  <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-semibold text-green-700">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M20 6L9 17l-5-5" />
                    </svg>
                    <span>Photo Attached Successfully</span>
                  </div>
                  <p className="mt-1 text-xs font-medium text-slate-700 truncate max-w-xs">
                    {imageFile?.name}
                  </p>
                  <p className="text-[11px] text-slate-400">
                    {((imageFile?.size || 0) / (1024 * 1024)).toFixed(2)} MB
                  </p>

                  <button
                    type="button"
                    onClick={handleRemoveImage}
                    disabled={isSubmitting}
                    className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-red-600 hover:text-red-700 underline"
                  >
                    Remove & Take Another
                  </button>
                </div>
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
              className={`flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed p-6 text-center transition-all duration-150 active:scale-[0.99] ${
                isDragging
                  ? 'border-teal-500 bg-teal-50'
                  : fieldErrors.image
                  ? 'border-red-300 bg-red-50/20'
                  : 'border-slate-300 bg-slate-50/50 hover:border-teal-400 hover:bg-teal-50/30'
              } ${isSubmitting ? 'cursor-not-allowed opacity-60' : ''}`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/jpg,image/png"
                capture="environment"
                className="sr-only"
                disabled={isSubmitting}
                onChange={(e) => handleImageSelect(e.target.files?.[0])}
              />
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-teal-100 text-teal-700">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z" />
                  <circle cx="12" cy="13" r="4" />
                </svg>
              </div>
              <p className="mt-2.5 text-sm font-semibold text-navy-900">
                <span className="text-teal-700 underline">Tap to take photo</span> or upload from device
              </p>
              <p className="mt-0.5 text-xs text-slate-500">
                Supports Camera, JPG or PNG (up to 10MB)
              </p>
            </div>
          )}
          {fieldErrors.image && <p className="field-error">{fieldErrors.image}</p>}
        </div>

        {/* 3. GPS Location Detection */}
        <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 sm:p-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <span className="text-sm font-bold text-navy-900">
                3. Exact Location <span className="text-teal-700">*</span>
              </span>
              <p className="text-xs text-slate-500">
                Enables municipal crew to navigate straight to the issue.
              </p>
            </div>

            <button
              type="button"
              onClick={handleDetectLocation}
              disabled={isSubmitting || locationStatus === 'detecting'}
              className="btn-accent inline-flex items-center justify-center gap-2 py-2 px-3 text-xs font-semibold shadow-sm disabled:opacity-60"
            >
              {locationStatus === 'detecting' ? (
                <>
                  <Spinner size={14} />
                  <span>Acquiring GPS Signal…</span>
                </>
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <circle cx="12" cy="12" r="10" />
                    <circle cx="12" cy="12" r="3" />
                    <line x1="12" y1="2" x2="12" y2="6" />
                    <line x1="12" y1="18" x2="12" y2="22" />
                    <line x1="2" y1="12" x2="6" y2="12" />
                    <line x1="18" y1="12" x2="22" y2="12" />
                  </svg>
                  <span>{latitude ? 'Update My Location' : 'Detect My Location'}</span>
                </>
              )}
            </button>
          </div>

          {/* Location Success Banner */}
          {locationStatus === 'success' && latitude && longitude && (
            <div className="mt-3 flex items-center justify-between rounded-lg border border-teal-200 bg-teal-50/80 px-3.5 py-2.5 text-xs text-teal-900">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-teal-600 text-white text-[10px]">✓</span>
                <span className="font-medium">
                  GPS Fixed: <strong className="font-mono">{latitude}, {longitude}</strong>
                </span>
              </div>
              <a
                href={`https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`}
                target="_blank"
                rel="noreferrer"
                className="font-semibold text-teal-800 hover:underline"
              >
                View Pin &rarr;
              </a>
            </div>
          )}

          {locationError && (
            <p className="mt-2.5 text-xs font-medium text-red-600 bg-red-50 p-2 rounded border border-red-200">
              {locationError}
            </p>
          )}

          {/* Coordinates (Expandable / Visible when needed) */}
          {(showManualCoords || (!latitude && !longitude)) && (
            <div className="mt-3 grid grid-cols-2 gap-3 pt-2 border-t border-slate-200">
              <div>
                <label htmlFor="latitude" className="field-label text-[11px]">
                  Latitude <span className="text-teal-700">*</span>
                </label>
                <input
                  id="latitude"
                  type="number"
                  step="any"
                  className={`input-field py-2 text-xs font-mono ${fieldErrors.location ? 'input-field-error' : ''}`}
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
                <label htmlFor="longitude" className="field-label text-[11px]">
                  Longitude <span className="text-teal-700">*</span>
                </label>
                <input
                  id="longitude"
                  type="number"
                  step="any"
                  className={`input-field py-2 text-xs font-mono ${fieldErrors.location ? 'input-field-error' : ''}`}
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
          )}
          {fieldErrors.location && <p className="field-error">{fieldErrors.location}</p>}

          {/* Optional Landmark / Address */}
          <div className="mt-3">
            <label htmlFor="address" className="field-label text-xs">
              Landmark / Street Address (Recommended)
            </label>
            <input
              id="address"
              type="text"
              className={`input-field py-2 text-xs ${fieldErrors.address ? 'input-field-error' : ''}`}
              placeholder="e.g. Opposite Sector 14 Gate 2, near Municipal Water Tank"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              disabled={isSubmitting}
              maxLength={500}
            />
            {fieldErrors.address && <p className="field-error">{fieldErrors.address}</p>}
          </div>
        </div>

        {/* 4. Description Textarea */}
        <div>
          <div className="flex items-center justify-between">
            <label htmlFor="description" className="field-label">
              4. Issue Description <span className="text-teal-700">*</span>
            </label>
            <span className={`text-xs ${description.length > 5000 ? 'text-red-600 font-bold' : 'text-slate-400'}`}>
              {description.length} / 5000
            </span>
          </div>

          <textarea
            id="description"
            rows={4}
            className={`input-field resize-y text-sm ${fieldErrors.description ? 'input-field-error' : ''}`}
            placeholder="Describe the issue clearly (e.g. depth of pothole, traffic obstruction, since when it has been occurring)..."
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
          <p className="mt-1 text-[11px] text-slate-400">
            Minimum 10 characters. Can be written in English, Hindi, or Hinglish.
          </p>
        </div>

        {/* Primary Submit Button */}
        <div className="pt-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary w-full py-3 text-sm font-semibold tracking-wide shadow-md active:scale-[0.99] disabled:opacity-60"
          >
            {isSubmitting ? (
              <span className="flex items-center justify-center gap-2">
                <Spinner size={18} />
                <span>Uploading & Submitting Report…</span>
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                <span>Submit Civic Report</span>
                <span aria-hidden="true">&rarr;</span>
              </span>
            )}
          </button>
          <p className="mt-2 text-center text-[11px] text-slate-400">
            Your report is sent directly to the local municipal ward officer.
          </p>
        </div>
      </form>
    </div>
  )
}
