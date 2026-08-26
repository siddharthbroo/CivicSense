import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

import ProtectedRoute from './ProtectedRoute.jsx'
import AppShell from '../components/layout/AppShell.jsx'

import Login from '../pages/auth/Login.jsx'
import Register from '../pages/auth/Register.jsx'
import IdentityUpload from '../pages/registration/IdentityUpload.jsx'
import IdentityReview from '../pages/registration/IdentityReview.jsx'
import MobileVerification from '../pages/registration/MobileVerification.jsx'
import OtpVerification from '../pages/registration/OtpVerification.jsx'
import CreatePassword from '../pages/registration/CreatePassword.jsx'
import RegistrationSuccess from '../pages/registration/RegistrationSuccess.jsx'

import Dashboard from '../pages/dashboard/Dashboard.jsx'
import Unauthorized from '../pages/errors/Unauthorized.jsx'
import Forbidden from '../pages/errors/Forbidden.jsx'

export default function AppRoutes() {
  const { isAuthenticated } = useAuth()

  return (
    <Routes>
      {/* Public: auth entry points */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <Login />}
      />

      <Route path="/register" element={<Register />}>
        <Route index element={<Navigate to="identity" replace />} />
        <Route path="identity" element={<IdentityUpload />} />
        <Route path="review" element={<IdentityReview />} />
        <Route path="mobile" element={<MobileVerification />} />
        {/* /register/verify satisfies the required route contract;
            /register/otp is kept as an alias since the recommended
            flow docs refer to this step as "OTP Verification". */}
        <Route path="verify" element={<OtpVerification />} />
        <Route path="otp" element={<Navigate to="/register/verify" replace />} />
        <Route path="password" element={<CreatePassword />} />
        <Route path="success" element={<RegistrationSuccess />} />
      </Route>

      {/* Error/status pages */}
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="/forbidden" element={<Forbidden />} />

      {/* Authenticated app */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/dashboard" element={<Dashboard />} />

          {/* Role-specific route stubs for future expansion. All three
              currently render the same role-aware Dashboard; split
              these into dedicated pages as citizen/officer/admin
              features are built out. */}
          <Route path="/citizen" element={<Dashboard />} />
          <Route path="/officer" element={<Dashboard />} />
          <Route path="/admin" element={<Dashboard />} />
        </Route>
      </Route>

      <Route path="/" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
      <Route path="*" element={<Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />} />
    </Routes>
  )
}
