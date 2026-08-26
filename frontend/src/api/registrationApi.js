import { apiRequest } from './api.js'

/**
 * Completes registration once mobile number has been verified via OTP.
 *
 * Backend: POST /users/register (application/json)
 *
 * @param {{otpVerificationId: string, password: string}} payload
 * @returns {Promise<{userId:string, name:string, mobileNumber:string, role:string, status:string, message:string}>}
 */
export function registerUser({ otpVerificationId, password }) {
  return apiRequest('/users/register', {
    method: 'POST',
    body: { otpVerificationId, password },
  })
}
