import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react'
import {
  getStoredToken,
  setStoredToken,
  clearStoredToken,
  onUnauthorized,
} from '../api/api.js'

const USER_STORAGE_KEY = 'civicsense_user'

const AuthContext = createContext(null)

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * Owns all authentication state for the app: the JWT, the logged-in
 * user's profile/role, and login/logout actions. Pages and components
 * consume this via useAuth() instead of touching localStorage or the
 * token directly.
 */
export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getStoredToken())
  const [user, setUser] = useState(() => readStoredUser())
  const [isInitializing, setIsInitializing] = useState(false)

  const isAuthenticated = Boolean(token)

  const login = useCallback((loginResponse) => {
    const { token: jwt, ...profile } = loginResponse
    setStoredToken(jwt)
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(profile))
    setToken(jwt)
    setUser(profile)
  }, [])

  const logout = useCallback(() => {
    clearStoredToken()
    localStorage.removeItem(USER_STORAGE_KEY)
    setToken(null)
    setUser(null)
  }, [])

  // Any API call that receives a 401 should end the session, wherever
  // in the app it happened.
  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      clearStoredToken()
      localStorage.removeItem(USER_STORAGE_KEY)
      setToken(null)
      setUser(null)
    })
    return unsubscribe
  }, [])

  const value = useMemo(
    () => ({
      token,
      user,
      role: user?.role ?? null,
      isAuthenticated,
      isInitializing,
      login,
      logout,
    }),
    [token, user, isAuthenticated, isInitializing, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth() must be used within an AuthProvider')
  }
  return ctx
}
