/**
 * Centralized API configuration.
 *
 * This is the ONLY place the backend host and API prefix are defined.
 * Every other file in the app (components, pages, other api/* modules)
 * must go through the helpers exported here instead of building URLs
 * or calling fetch() directly.
 */

const API_HOST = 'http://localhost:8080'
const API_PREFIX = '/api/v1'

export const BASE_URL = `${API_HOST}${API_PREFIX}`

const TOKEN_STORAGE_KEY = 'civicsense_token'

/** Read the persisted JWT, if any. */
export function getStoredToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

/** Persist the JWT after a successful login. */
export function setStoredToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token)
  }
}

/** Remove the JWT — used on logout / 401. */
export function clearStoredToken() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
}

/**
 * A normalized error thrown by apiRequest so UI code can rely on a
 * consistent shape instead of catching raw fetch/TypeError objects.
 */
export class ApiError extends Error {
  constructor(message, { status, details } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

/**
 * Fires when the server responds 401 Unauthorized. The AuthContext
 * subscribes to this so a session can be terminated from anywhere in
 * the app (not just from within a component that made the call).
 */
const unauthorizedListeners = new Set()

export function onUnauthorized(listener) {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

function notifyUnauthorized() {
  unauthorizedListeners.forEach((listener) => listener())
}

/**
 * Core request helper. Every api/*.js module builds on top of this
 * function rather than calling fetch() directly.
 *
 * @param {string} path - path relative to BASE_URL, e.g. '/auth/login'
 * @param {object} options
 * @param {string} [options.method]
 * @param {object|FormData} [options.body]
 * @param {boolean} [options.auth] - attach the Authorization header
 * @param {object} [options.headers] - extra headers to merge in
 */
export async function apiRequest(
  path,
  { method = 'GET', body, auth = false, headers = {} } = {},
) {
  const isFormData = body instanceof FormData
  const finalHeaders = { ...headers }

  if (!isFormData && body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json'
  }

  if (auth) {
    const token = getStoredToken()
    if (token) {
      finalHeaders['Authorization'] = `Bearer ${token}`
    }
  }

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: finalHeaders,
      body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
    })
  } catch {
    // Network failure, backend down, CORS block, etc. Never surface the
    // raw browser error message to the user.
    throw new ApiError('Unable to connect to the server. Please try again.', {
      status: 0,
    })
  }

  if (response.status === 401) {
    notifyUnauthorized()
  }

  let payload = null
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    payload = await response.json().catch(() => null)
  }

  if (!response.ok) {
    const message =
      (payload && (payload.message || payload.error)) ||
      defaultMessageForStatus(response.status)

    throw new ApiError(message, { status: response.status, details: payload })
  }

  return payload
}

function defaultMessageForStatus(status) {
  switch (status) {
    case 400:
      return 'The request could not be processed. Please check your input.'
    case 401:
      return 'Your session has expired. Please log in again.'
    case 403:
      return 'You do not have permission to perform this action.'
    case 404:
      return 'The requested resource could not be found.'
    case 409:
      return 'This request conflicts with existing data.'
    case 500:
    case 502:
    case 503:
      return 'The server encountered a problem. Please try again shortly.'
    default:
      return 'Something went wrong. Please try again.'
  }
}
