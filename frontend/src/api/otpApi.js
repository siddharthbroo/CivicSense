import { apiRequest } from './api.js'

/**
 * Sends an OTP to the user's mobile number.
 *
 * Backend:
 * POST /api/v1/otp/send
 *
 * @param {{mobileNumber: string, purpose?: string}} payload
 * @returns {Promise<{otpVerificationId: string, expiresAt: string, message: string}>}
 */
export function sendOtp({ mobileNumber, purpose = 'REGISTRATION' }) {
  return apiRequest('/otp/send', {
    method: 'POST',
    body: { mobileNumber, purpose },
  })
}

/**
 * Verifies the OTP entered by the user.
 *
 * Backend:
 * POST /api/v1/otp/verify
 *
 * @param {{otpVerificationId: string, otp: string}} payload
 * @returns {Promise<{verified: boolean, message: string}>}
 */
export function verifyOtp({ otpVerificationId, otp }) {
  return apiRequest('/otp/verify', {
    method: 'POST',
    body: { otpVerificationId, otp },
  })
}

/**
 * Resends OTP by calling the send endpoint again.
 *
 * @param {{mobileNumber: string}} payload
 * @returns {Promise<{otpVerificationId: string, expiresAt: string, message: string}>}
 */
export function resendOtp({ mobileNumber }) {
  return sendOtp({ mobileNumber })
}