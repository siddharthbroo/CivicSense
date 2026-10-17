import { apiRequest } from './api.js'

/**
 * Creates the citizen account after successful OTP verification.
 *
 * Backend: POST /api/v1/users/register
 *
 * @param {{otpVerificationId: string, name: string, email: string, password: string}} payload
 * @returns {Promise<{userId: string, name: string, mobileNumber: string, role: string, status: string, message: string}>}
 */
export function registerUser({ otpVerificationId, name, email, password }) {
  return apiRequest('/users/register', {
    method: 'POST',
    body: { otpVerificationId, name, email, password },
  })
}