import { createContext, useContext, useMemo, useState } from 'react'

const RegistrationContext = createContext(null)

const initialState = {
  // Step 1: Account details
  name: '',
  email: '',
  mobileNumber: '',
  password: '',        // held in-memory only; never persisted
  // Step 1 → 2: OTP
  otpVerificationId: null,
  // Step 2 → 3: created account
  registeredUser: null,
}

/**
 * Holds the in-progress registration data as the user moves through
 * /register → /register/verify → /register/success.
 * Scoped to the /register route subtree only (see AppRoutes.jsx)
 * so it resets automatically on remount.
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