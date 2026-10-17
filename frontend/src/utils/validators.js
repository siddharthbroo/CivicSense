export function isRequired(value) {
  return value !== undefined && value !== null && String(value).trim().length > 0
}

export function isValidMobileNumber(value) {
  return /^[6-9]\d{9}$/.test(value.trim())
}

export function isValidPassword(value) {
  return value.length >= 8
}

export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}

/**
 * Validates the full name field, returning an error string or ''.
 */
export function validateName(value) {
  if (!isRequired(value)) return 'Full name is required.'
  if (String(value).trim().length < 2) return 'Name must be at least 2 characters.'
  return ''
}

/**
 * Validates the email field, returning an error string or ''.
 */
export function validateEmail(value) {
  if (!isRequired(value)) return 'Email address is required.'
  if (!isValidEmail(value)) return 'Enter a valid email address.'
  return ''
}

/**
 * Validates the mobile number field, returning an error string or ''.
 */
export function validateMobileNumber(value) {
  if (!isRequired(value)) return 'Mobile number is required.'
  if (!isValidMobileNumber(value)) return 'Enter a valid 10-digit mobile number.'
  return ''
}

/**
 * Validates password + confirmation together, returning
 * { password, confirmPassword } error strings (empty when valid).
 */
export function validatePasswordPair(password, confirmPassword) {
  const errors = { password: '', confirmPassword: '' }

  if (!isRequired(password)) {
    errors.password = 'Password is required.'
  } else if (!isValidPassword(password)) {
    errors.password = 'Password must be at least 8 characters.'
  }

  if (!isRequired(confirmPassword)) {
    errors.confirmPassword = 'Please confirm your password.'
  } else if (password !== confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.'
  }

  return errors
}