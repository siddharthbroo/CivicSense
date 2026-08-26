import { apiRequest } from './api.js'

/**
 * Logs a user in.
 *
 * Backend: POST /auth/login (application/json)
 *
 * @param {{mobileNumber: string, password: string}} credentials
 * @returns {Promise<{userId:string, name:string, mobileNumber:string, role:string, status:string, token:string, message:string}>}
 */
export function login({ mobileNumber, password }) {
  return apiRequest('/auth/login', {
    method: 'POST',
    body: { mobileNumber, password },
  })
}
