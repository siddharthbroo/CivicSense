import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/**
 * Restricts a route subtree to one or more roles. This is a UI/UX
 * convenience only — real authorization is enforced by Spring Security
 * on the backend using the JWT's role claim.
 *
 * @param {{allowedRoles: string[]}} props
 */
export default function RoleRoute({ allowedRoles }) {
  const { isAuthenticated, role } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (!allowedRoles.includes(role)) {
    return <Navigate to="/forbidden" replace />
  }

  return <Outlet />
}
