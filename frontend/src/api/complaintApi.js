import { apiRequest } from './api.js'

/**
 * Submits a new citizen civic complaint to the backend.
 *
 * Backend: POST /api/v1/complaints (multipart/form-data)
 *
 * @param {Object} payload
 * @param {string} payload.description - Complaint description (10-5000 chars)
 * @param {number} payload.latitude - Confirmed incident location latitude (Primary)
 * @param {number} payload.longitude - Confirmed incident location longitude (Primary)
 * @param {string} [payload.address] - Optional detailed address/landmark (up to 500 chars)
 * @param {File} [payload.image] - Optional photo of the civic issue
 * @param {number} [payload.deviceLatitude] - Optional device GPS latitude at submission
 * @param {number} [payload.deviceLongitude] - Optional device GPS longitude at submission
 * @param {string} [payload.locationSource] - Source of incident location (e.g. MANUAL_PIN, DEVICE_GPS_SUGGESTED)
 * @param {number} [payload.locationAccuracy] - Location accuracy in meters
 * @returns {Promise<{complaintId: string, status: string, createdAt: string, message: string}>}
 */
export function createComplaint({
  description,
  latitude,
  longitude,
  address,
  image,
  deviceLatitude,
  deviceLongitude,
  locationSource,
  locationAccuracy,
}) {
  const formData = new FormData()

  formData.append('description', description)
  formData.append('latitude', latitude)
  formData.append('longitude', longitude)

  if (address && address.trim()) {
    formData.append('address', address.trim())
  }

  if (image) {
    formData.append('image', image)
  }

  if (deviceLatitude !== undefined && deviceLatitude !== null) {
    formData.append('deviceLatitude', deviceLatitude)
  }

  if (deviceLongitude !== undefined && deviceLongitude !== null) {
    formData.append('deviceLongitude', deviceLongitude)
  }

  if (locationSource) {
    formData.append('locationSource', locationSource)
  }

  if (locationAccuracy !== undefined && locationAccuracy !== null) {
    formData.append('locationAccuracy', locationAccuracy)
  }

  return apiRequest('/complaints', {
    method: 'POST',
    body: formData,
    auth: true,
  })
}

/**
 * Fetches complaints submitted by the currently authenticated citizen.
 *
 * Backend: GET /api/v1/complaints/my
 *
 * @returns {Promise<Array>}
 */
export function getMyComplaints() {
  return apiRequest('/complaints/my', {
    method: 'GET',
    auth: true,
  })
}

/**
 * Validates file integrity, safety, and complaint relevance via backend.
 *
 * Backend: POST /api/v1/complaints/validate-image (multipart/form-data)
 *
 * @param {File} imageFile
 * @param {string} [category]
 * @param {string} [description]
 * @returns {Promise<{valid: boolean, prohibited: boolean, unrelated: boolean, quarantined: boolean, reason: string, category: string, relevanceStatus: string, confidence: number}>}
 */
export function validateComplaintImage(imageFile, category, description) {
  const formData = new FormData()
  formData.append('image', imageFile)
  if (category) {
    formData.append('category', category)
  }
  if (description && description.trim()) {
    formData.append('description', description.trim())
  }

  return apiRequest('/complaints/validate-image', {
    method: 'POST',
    body: formData,
    auth: true,
  })
}