export function isRequired(value) {
  return value !== undefined && value !== null && String(value).trim().length > 0
}

export function isValidMobileNumber(value) {
  return /^[6-9]\d{9}$/.test(value.trim())
}

export function isValidPassword(value) {
  return value.length >= 8
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
