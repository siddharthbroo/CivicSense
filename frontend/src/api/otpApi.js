import { apiRequest } from './api.js'

/**
 * Sends an OTP to the user's mobile number.
 *
 * Backend:
 * POST /api/v1/otp/send
 *
 * @param {{identityVerificationId: string, mobileNumber: string}} payload
 */
export function sendOtp({ identityVerificationId, mobileNumber }) {
  return apiRequest('/otp/send', {
    method: 'POST',
    body: {
      identityVerificationId,
      mobileNumber,
    },
  })
}

/**
 * Verifies the OTP entered by the user.
 *
 * Backend:
 * POST /api/v1/otp/verify
 *
 * @param {{otpVerificationId: string, otp: string}} payload
 */
export function verifyOtp({ otpVerificationId, otp }) {
  return apiRequest('/otp/verify', {
    method: 'POST',
    body: {
      otpVerificationId,
      otp,
    },
  })
}

/**
 * Resends OTP using the existing send OTP endpoint.
 *
 * Backend has no separate /resend endpoint.
 *
 * @param {{identityVerificationId: string, mobileNumber: string}} payload
 */
export function resendOtp({ identityVerificationId, mobileNumber }) {
  return sendOtp({
    identityVerificationId,
    mobileNumber,
  })
}