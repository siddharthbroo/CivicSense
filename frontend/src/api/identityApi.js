import { apiRequest } from './api.js'

/**
 * Uploads an identity document for OCR processing.
 *
 * Backend: POST /identity-verifications/document (multipart/form-data)
 *
 * @param {File} documentFile
 * @param {'AADHAAR'|'PAN'|'PASSPORT'|'DRIVING_LICENSE'|'COLLEGE_ID'} documentType
 * @returns {Promise<{verificationId:string, documentType:string, ocrStatus:string, extractedName:string, extractedDob:string, extractedGender:string}>}
 */
export function uploadIdentityDocument(documentFile, documentType) {
  const formData = new FormData()
  formData.append('document', documentFile)
  formData.append('documentType', documentType)

  return apiRequest('/identity-verifications/document', {
    method: 'POST',
    body: formData,
  })
}

/**
 * Confirms the (possibly user-edited) OCR-extracted identity details.
 *
 * Backend: POST /identity-verifications/confirm (application/json)
 *
 * @param {{verificationId: string, name: string, dateOfBirth: string, gender: string}} payload
 */
export function confirmIdentity({ verificationId, name, dateOfBirth, gender }) {
  return apiRequest('/identity-verifications/confirm', {
    method: 'POST',
    body: { verificationId, name, dateOfBirth, gender },
  })
}

export const DOCUMENT_TYPES = [
  { value: 'AADHAAR', label: 'Aadhaar Card' },
  { value: 'PAN', label: 'PAN Card' },
  { value: 'PASSPORT', label: 'Passport' },
  { value: 'DRIVING_LICENSE', label: 'Driving License' },
  { value: 'COLLEGE_ID', label: 'College ID' },
]
