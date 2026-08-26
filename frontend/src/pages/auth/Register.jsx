import { Outlet } from 'react-router-dom'
import { RegistrationProvider } from '../../context/RegistrationContext.jsx'

/**
 * Parent route for the whole /register/* flow. Wraps every step in
 * RegistrationProvider so data collected in step 1 (e.g. verificationId)
 * is available in later steps without prop-drilling through the router.
 */
export default function Register() {
  return (
    <RegistrationProvider>
      <Outlet />
    </RegistrationProvider>
  )
}
