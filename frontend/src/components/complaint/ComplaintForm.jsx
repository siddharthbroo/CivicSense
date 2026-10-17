import { useState, useRef, useEffect, useCallback } from 'react'
import Alert from '../common/Alert.jsx'
import Spinner from '../common/Spinner.jsx'
import { createComplaint, validateComplaintImage } from '../../api/complaintApi.js'
import NearbyCommunityComplaints from './NearbyCommunityComplaints.jsx'
import LocationPickerModal from './LocationPickerModal.jsx'
import { validateCoordinates } from '../../utils/locationValidation.js'

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

  // Incident Location (Primary Map Pin coordinates)
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [locationSource, setLocationSource] = useState('MANUAL_PIN')
  const [locationAccuracy, setLocationAccuracy] = useState(null)

  // Device GPS telemetry (optional hint)
  const [deviceCoords, setDeviceCoords] = useState(null)
  const [suggestedCoords, setSuggestedCoords] = useState(null)

  const [showManualCoords, setShowManualCoords] = useState(false)
  const [locationStatus, setLocationStatus] = useState('idle') // idle | detecting | success | error
  const [locationError, setLocationError] = useState('')
  const [isMapPickerOpen, setIsMapPickerOpen] = useState(false)

  // Optional photo & moderation state
  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [isDragging, setIsDragging] = useState(false)
  const [imageModerationStatus, setImageModerationStatus] = useState('idle') // idle | validating | approved | prohibited | quarantined | error
  const [imageModerationMessage, setImageModerationMessage] = useState('')

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

  // Handle explicit confirmation from interactive map modal
  function handleConfirmMapLocation({ latitude: confirmedLat, longitude: confirmedLng }) {
    setLatitude(confirmedLat)
    setLongitude(confirmedLng)
    setLocationSource('MAP_PIN')
    setLocationStatus('confirmed')
    setFieldErrors((prev) => ({ ...prev, location: '' }))
    setIsMapPickerOpen(false)
  }

  // Suggest location from device GPS
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
        const acc = position.coords.accuracy ? Number(position.coords.accuracy.toFixed(1)) : null

        // GPS is a suggestion only. The citizen must explicitly confirm it
        // before it becomes the primary incident map pin.
        setSuggestedCoords({ latitude: lat, longitude: lng })
        setLocationAccuracy(acc)

        // Store device telemetry
        setDeviceCoords({ latitude: lat, longitude: lng })

        setLocationStatus('success')
      },
      (error) => {
        setLocationStatus('error')
        setShowManualCoords(true)
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setLocationError('Location permission denied. Please enter coordinates or place a pin manually.')
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

  function handleConfirmSuggestedLocation() {
    if (!suggestedCoords) return

    setLatitude(suggestedCoords.latitude)
    setLongitude(suggestedCoords.longitude)
    setLocationSource('MAP_PIN')
    setLocationStatus('confirmed')
    setFieldErrors((prev) => ({ ...prev, location: '' }))
  }

  // Screen image safety, file integrity, and complaint relevance
  const screenImage = useCallback(async (file, cat, desc) => {
    if (!file) return

    setImageModerationStatus('validating')
    setImageModerationMessage('Screening photo safety & complaint relevance…')

    try {
      const result = await validateComplaintImage(file, cat, desc)

      if (!result.valid) {
        setImageModerationStatus('error')
        setImageModerationMessage(result.reason || 'Image file is invalid or corrupted.')
        setFieldErrors((prev) => ({ ...prev, image: result.reason }))
      } else if (result.prohibited) {
        setImageModerationStatus('prohibited')
        setImageModerationMessage(
          `Image prohibited: ${result.reason || 'Violates civic platform safety policy.'}. Please remove or replace this photo.`
        )
        setFieldErrors((prev) => ({
          ...prev,
          image: `Prohibited content (${result.category}): ${result.reason}`,
        }))
      } else if (result.unrelated) {
        setImageModerationStatus('unrelated')
        setImageModerationMessage(
          `Photo appears unrelated to this complaint: ${result.reason || 'Please attach a photo showing the civic problem, or remove the photo.'}`
        )
        setFieldErrors((prev) => ({
          ...prev,
          image: `Unrelated photo: ${result.reason || 'Please upload a photo of the reported civic issue.'}`,
        }))
      } else if (result.quarantined) {
        setImageModerationStatus('quarantined')
        setImageModerationMessage(
          result.reason || 'Photo flagged for verification. Review is required before submitting with this image.'
        )
        setFieldErrors((prev) => ({
          ...prev,
          image: result.reason || 'Image verification is uncertain. Please remove the image or take a clearer photo to submit.',
        }))
      } else {
        setImageModerationStatus('approved')
        setImageModerationMessage('Photo verified and relevant to this complaint.')
        setFieldErrors((prev) => ({ ...prev, image: '' }))
      }
    } catch {
      // In case network or server endpoint is unreachable -> fail-safe quarantine
      setImageModerationStatus('quarantined')
      setImageModerationMessage('Image verification service unavailable. Please retry or remove photo to submit text report.')
      setFieldErrors((prev) => ({
        ...prev,
        image: 'Image verification unavailable. Please remove photo or retry.',
      }))
    }
  }, [])

  // Image selection validation & screening
  function handleImageSelect(file) {
    if (!file) return

    // 1. Client-side format check
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type.toLowerCase())) {
      setFieldErrors((prev) => ({
        ...prev,
        image: 'Only JPG, JPEG, or PNG images are supported.',
      }))
      setImageModerationStatus('error')
      setImageModerationMessage('Unsupported format. Please upload a JPG or PNG file.')
      return
    }

    // 2. Client-side size check
    if (file.size > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
      setFieldErrors((prev) => ({
        ...prev,
        image: `Image size must not exceed ${MAX_IMAGE_SIZE_MB}MB.`,
      }))
      setImageModerationStatus('error')
      setImageModerationMessage(`File exceeds ${MAX_IMAGE_SIZE_MB}MB limit.`)
      return
    }

    setFieldErrors((prev) => ({ ...prev, image: '' }))
    setImageFile(file)
    const previewUrl = URL.createObjectURL(file)
    setImagePreview(previewUrl)

    // Run backend screening
    screenImage(file, selectedCategory, description)
  }

  // Revalidate image when user changes complaint category or description (debounced)
  useEffect(() => {
    if (!imageFile) return

    const timer = setTimeout(() => {
      screenImage(imageFile, selectedCategory, description)
    }, 600)

    return () => clearTimeout(timer)
  }, [imageFile, selectedCategory, description, screenImage])

  function handleRemoveImage() {
    if (imagePreview) {
      URL.revokeObjectURL(imagePreview)
    }
    setImageFile(null)
    setImagePreview(null)
    setImageModerationStatus('idle')
    setImageModerationMessage('')
    setFieldErrors((prev) => ({ ...prev, image: '' }))
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
      errors.location = 'Incident location is required. Tap "Detect Current Location" to suggest coordinates or enter them below.'
    } else {
      const coordResult = validateCoordinates(latitude, longitude, selectedCategory)
      if (!coordResult.isValid) {
        errors.location = coordResult.error
      }
    }

    // Photo is OPTIONAL: No mandatory error for missing image.
    // However, if an image IS provided, it must not be invalid, prohibited, unrelated, quarantined/uncertain, or actively validating.
    if (imageFile) {
      if (imageModerationStatus === 'validating') {
        errors.image = 'Please wait while the image safety & relevance screening completes.'
      } else if (imageModerationStatus === 'prohibited') {
        errors.image = imageModerationMessage || 'This photo contains prohibited content and cannot be submitted.'
      } else if (imageModerationStatus === 'unrelated') {
        errors.image = imageModerationMessage || 'This photo appears unrelated to the reported civic complaint.'
      } else if (imageModerationStatus === 'quarantined') {
        errors.image = imageModerationMessage || 'Image verification is uncertain. Please remove the photo or follow review requirements.'
      } else if (imageModerationStatus === 'error' && fieldErrors.image) {
        errors.image = fieldErrors.image
      }
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
        image: imageFile || undefined,
        deviceLatitude: deviceCoords?.latitude,
        deviceLongitude: deviceCoords?.longitude,
        locationSource,
        locationAccuracy,
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
        imageUrl: imagePreview || null,
      })

      // Reset form
      setDescription('')
      setAddress('')
      setLatitude('')
      setLongitude('')
      setSelectedCategory(null)
      setLocationStatus('idle')
      setDeviceCoords(null)
      setSuggestedCoords(null)
      setLocationAccuracy(null)
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
              Submit your complaint with a confirmed incident location. Photos are optional.
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

        {/* 2. Confirmed Incident Location (Primary) */}
        <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 sm:p-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <span className="text-sm font-bold text-navy-900">
                2. Incident Location (Map Pin) <span className="text-teal-700">*</span>
              </span>
              <p className="text-xs text-slate-500">
                The exact location where the issue occurred.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setIsMapPickerOpen(true)}
                disabled={isSubmitting}
                className="btn-secondary inline-flex items-center justify-center gap-1.5 py-2 px-3 text-xs font-semibold shadow-sm hover:border-teal-500 hover:text-teal-700"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                  <circle cx="12" cy="10" r="3" />
                </svg>
                <span>Choose on Map</span>
              </button>

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
                    <span>{suggestedCoords ? 'Refresh Current Location' : 'Detect Current Location'}</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Location Success Banner */}
          {locationStatus === 'success' && suggestedCoords && (
            <div className="mt-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 rounded-xl border border-teal-200 bg-teal-50/80 p-3 text-xs text-teal-900">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-teal-600 text-white text-[10px] shrink-0">✓</span>
                <span className="font-medium">
                  Suggested location: <strong className="font-mono">{suggestedCoords.latitude}, {suggestedCoords.longitude}</strong>
                </span>
              </div>
              <div className="flex items-center gap-3 shrink-0 pt-1 sm:pt-0">
                <button
                  type="button"
                  onClick={() => setIsMapPickerOpen(true)}
                  className="font-semibold text-teal-800 hover:underline cursor-pointer flex items-center gap-1"
                >
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                    <line x1="8" y1="2" x2="8" y2="18" />
                    <line x1="16" y1="6" x2="16" y2="22" />
                  </svg>
                  <span>View on Map</span>
                </button>
                <button
                  type="button"
                  onClick={handleConfirmSuggestedLocation}
                  disabled={isSubmitting}
                  className="font-semibold text-teal-800 hover:underline cursor-pointer"
                >
                  Use as incident pin
                </button>
              </div>
            </div>
          )}

          {locationStatus === 'confirmed' && latitude && longitude && (
            <div className="mt-3 flex items-center justify-between rounded-lg border border-teal-200 bg-teal-50/80 px-3.5 py-2.5 text-xs text-teal-900">
              <span className="font-medium">
                Confirmed incident pin: <strong className="font-mono">{latitude}, {longitude}</strong>
              </span>
              <button
                type="button"
                onClick={() => setIsMapPickerOpen(true)}
                className="font-semibold text-teal-800 hover:underline cursor-pointer flex items-center gap-1"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                  <line x1="8" y1="2" x2="8" y2="18" />
                  <line x1="16" y1="6" x2="16" y2="22" />
                </svg>
                <span>View / Adjust on Map</span>
              </button>
            </div>
          )}

          {locationError && (
            <p className="mt-2.5 text-xs font-medium text-red-600 bg-red-50 p-2 rounded border border-red-200">
              {locationError}
            </p>
          )}

          {/* Incident Pin Coordinates */}
          {(showManualCoords || (!latitude && !longitude) || locationStatus === 'success' || locationStatus === 'confirmed') && (
            <div className="mt-3 grid grid-cols-2 gap-3 pt-2 border-t border-slate-200">
              <div>
                <label htmlFor="latitude" className="field-label text-[11px]">
                  Incident Latitude <span className="text-teal-700">*</span>
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
                    setLocationSource('MANUAL_PIN')
                    if (fieldErrors.location) setFieldErrors((prev) => ({ ...prev, location: '' }))
                  }}
                  disabled={isSubmitting}
                />
              </div>
              <div>
                <label htmlFor="longitude" className="field-label text-[11px]">
                  Incident Longitude <span className="text-teal-700">*</span>
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
                    setLocationSource('MANUAL_PIN')
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

          {/* Nearby Community Reports Discovery (encourages "I'm affected too" support over duplicates) */}
          {latitude && longitude && !isNaN(parseFloat(latitude)) && !isNaN(parseFloat(longitude)) && (
            <div className="mt-4">
              <NearbyCommunityComplaints
                latitude={latitude}
                longitude={longitude}
              />
            </div>
          )}
        </div>

        {/* 3. Photo Upload (Optional) */}
        <div>
          <label className="field-label flex items-center justify-between">
            <span>
              3. Issue Photograph <span className="text-xs font-normal text-slate-500">(Optional)</span>
            </span>
            <span className="text-[11px] text-slate-400">JPG, PNG up to {MAX_IMAGE_SIZE_MB}MB</span>
          </label>

          {imagePreview ? (
            <div className={`relative overflow-hidden rounded-xl border p-4 transition-all ${
              imageModerationStatus === 'prohibited' || imageModerationStatus === 'unrelated'
                ? 'border-red-300 bg-red-50/50'
                : imageModerationStatus === 'quarantined'
                ? 'border-amber-300 bg-amber-50/50'
                : imageModerationStatus === 'validating'
                ? 'border-teal-300 bg-teal-50/40'
                : 'border-slate-200 bg-slate-50'
            }`}>
              <div className="flex flex-col sm:flex-row items-center gap-4">
                <div className="relative h-28 w-28 shrink-0 overflow-hidden rounded-lg border border-slate-200 shadow-sm bg-white">
                  <img
                    src={imagePreview}
                    alt="Uploaded issue evidence"
                    className="h-full w-full object-cover"
                  />
                  {imageModerationStatus === 'validating' && (
                    <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center">
                      <Spinner size={24} className="text-white" />
                    </div>
                  )}
                </div>
                <div className="flex-1 text-center sm:text-left">
                  {/* Status Badges */}
                  {imageModerationStatus === 'validating' && (
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-semibold text-teal-700">
                      <span className="w-2 h-2 rounded-full bg-teal-500 animate-ping" />
                      <span>Screening Image Safety…</span>
                    </div>
                  )}

                  {imageModerationStatus === 'approved' && (
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-semibold text-green-700">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M20 6L9 17l-5-5" />
                      </svg>
                      <span>Photo Approved & Ready</span>
                    </div>
                  )}

                  {imageModerationStatus === 'prohibited' && (
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-bold text-red-700">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
                      </svg>
                      <span>Prohibited Content Detected</span>
                    </div>
                  )}

                  {imageModerationStatus === 'unrelated' && (
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-bold text-red-700">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                      </svg>
                      <span>Unrelated Photo Detected</span>
                    </div>
                  )}

                  {imageModerationStatus === 'quarantined' && (
                    <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs font-semibold text-amber-700">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                      </svg>
                      <span>Uncertain Image — Verification Required</span>
                    </div>
                  )}

                  <p className="mt-1 text-xs font-medium text-slate-700 truncate max-w-xs">
                    {imageFile?.name}
                  </p>
                  <p className="text-[11px] text-slate-400">
                    {((imageFile?.size || 0) / (1024 * 1024)).toFixed(2)} MB
                  </p>

                  {imageModerationMessage && (
                    <p className={`mt-1.5 text-xs font-medium ${
                      imageModerationStatus === 'prohibited' || imageModerationStatus === 'unrelated'
                        ? 'text-red-700 bg-red-100/60 border border-red-200 px-2 py-1 rounded-md'
                        : imageModerationStatus === 'quarantined'
                        ? 'text-amber-800 bg-amber-100/60 border border-amber-200 px-2 py-1 rounded-md'
                        : 'text-slate-500'
                    }`}>
                      {imageModerationMessage}
                    </p>
                  )}

                  <button
                    type="button"
                    onClick={handleRemoveImage}
                    disabled={isSubmitting}
                    className="mt-2.5 inline-flex items-center gap-1 text-xs font-semibold text-red-600 hover:text-red-700 underline cursor-pointer"
                  >
                    Remove Photo
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
                Optional · Supports Camera, JPG or PNG (up to 10MB)
              </p>
            </div>
          )}
          {fieldErrors.image && <p className="field-error">{fieldErrors.image}</p>}
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
          {imageModerationStatus === 'validating' && (
            <p className="mb-2 text-center text-xs font-semibold text-teal-700 bg-teal-50 border border-teal-200 py-1.5 px-3 rounded-lg animate-pulse">
              Please wait: verifying image file integrity, safety, and complaint relevance…
            </p>
          )}

          {imageModerationStatus === 'prohibited' && (
            <p className="mb-2 text-center text-xs font-semibold text-red-700 bg-red-50 border border-red-200 py-1.5 px-3 rounded-lg">
              Submission blocked: Remove or replace the prohibited photo before submitting.
            </p>
          )}

          {imageModerationStatus === 'unrelated' && (
            <p className="mb-2 text-center text-xs font-semibold text-red-700 bg-red-50 border border-red-200 py-1.5 px-3 rounded-lg">
              Submission blocked: The attached photo is unrelated to this complaint. Please remove or replace it.
            </p>
          )}

          {imageModerationStatus === 'quarantined' && (
            <p className="mb-2 text-center text-xs font-semibold text-amber-800 bg-amber-50 border border-amber-200 py-1.5 px-3 rounded-lg">
              Submission blocked with current image: Photo verification is uncertain. Please remove the photo to submit text-only, or take a clearer photo.
            </p>
          )}

          <button
            type="submit"
            disabled={
              isSubmitting ||
              imageModerationStatus === 'validating' ||
              imageModerationStatus === 'prohibited' ||
              imageModerationStatus === 'unrelated' ||
              imageModerationStatus === 'quarantined' ||
              Boolean(imageFile && fieldErrors.image)
            }
            className="btn-primary w-full py-3 text-sm font-semibold tracking-wide shadow-md active:scale-[0.99] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? (
              <span className="flex items-center justify-center gap-2">
                <Spinner size={18} />
                <span>Submitting Civic Report…</span>
              </span>
            ) : imageModerationStatus === 'validating' ? (
              <span className="flex items-center justify-center gap-2">
                <Spinner size={18} />
                <span>Screening Photo…</span>
              </span>
            ) : (
              <span className="flex items-center justify-center gap-2">
                <span>Submit Civic Report</span>
                <span aria-hidden="true">&rarr;</span>
              </span>
            )}
          </button>
          <p className="mt-2 text-center text-[11px] text-slate-400">
            Your report is recorded at the confirmed incident pin location.
          </p>
        </div>
      </form>

      {/* Interactive Map Location Picker Modal */}
      {isMapPickerOpen && (
        <LocationPickerModal
          isOpen={isMapPickerOpen}
          onClose={() => setIsMapPickerOpen(false)}
          onConfirm={handleConfirmMapLocation}
          initialLatitude={latitude}
          initialLongitude={longitude}
          suggestedLatitude={suggestedCoords?.latitude}
          suggestedLongitude={suggestedCoords?.longitude}
          category={selectedCategory}
        />
      )}
    </div>
  )
}
