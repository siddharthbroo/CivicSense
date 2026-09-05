import { apiRequest } from './api.js'

/**
 * Submits a new citizen civic complaint to the backend.
 *
 * Backend: POST /api/v1/complaints (multipart/form-data)
 *
 * @param {Object} payload
 * @param {string} payload.description - Complaint description (10-5000 chars)
 * @param {number} payload.latitude - Device/GPS latitude
 * @param {number} payload.longitude - Device/GPS longitude
 * @param {string} [payload.address] - Optional detailed address/landmark (up to 500 chars)
 * @param {File} payload.image - Mandatory photo of the civic issue
 * @returns {Promise<{complaintId: string, status: string, createdAt: string, message: string}>}
 */
export function createComplaint({ description, latitude, longitude, address, image }) {
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