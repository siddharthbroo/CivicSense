import { createContext, useContext, useMemo, useState } from 'react'

const RegistrationContext = createContext(null)

const initialState = {
  // Step 1 → 2: identity document + OCR result
  verificationId: null,
  documentType: '',
  name: '',
  dateOfBirth: '',
  gender: '',
  // Step 3 → 4: mobile + OTP
  mobileNumber: '',
  otpVerificationId: null,
}

/**
 * Holds the in-progress registration data as the user moves through
 * /register/identity → /register/success. Scoped to the registration
 * route subtree only (see AppRoutes.jsx) so it resets on remount.
 */
export function RegistrationProvider({ children }) {
  const [data, setData] = useState(initialState)

  const updateData = (patch) => setData((prev) => ({ ...prev, ...patch }))
  const resetData = () => setData(initialState)

  const value = useMemo(() => ({ data, updateData, resetData }), [data])

  return <RegistrationContext.Provider value={value}>{children}</RegistrationContext.Provider>
}

export function useRegistration() {
  const ctx = useContext(RegistrationContext)
  if (!ctx) {
    throw new Error('useRegistration() must be used within a RegistrationProvider')
  }
  return ctx
}
