import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

import ProtectedRoute from './ProtectedRoute.jsx'

import Login from '../pages/auth/Login.jsx'
import Register from '../pages/auth/Register.jsx'
import AccountDetails from '../pages/registration/AccountDetails.jsx'
import OtpVerification from '../pages/registration/OtpVerification.jsx'
import RegistrationSuccess from '../pages/registration/RegistrationSuccess.jsx'

import Home from '../pages/home/Home.jsx'
import ReportIssuePage from '../pages/complaint/ReportIssuePage.jsx'
import MyReportsPage from '../pages/complaint/MyReportsPage.jsx'
import CivicGuidelinesPage from '../pages/complaint/CivicGuidelinesPage.jsx'
import HowItWorksPage from '../pages/info/HowItWorksPage.jsx'
import AboutPage from '../pages/info/AboutPage.jsx'
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

      {/* Registration flow: /register → /register/verify → /register/success */}
      <Route path="/register" element={<Register />}>
        <Route index element={<AccountDetails />} />
        <Route path="verify" element={<OtpVerification />} />
        {/* /register/otp is an alias kept for any old links */}
        <Route path="otp" element={<Navigate to="/register/verify" replace />} />
        <Route path="success" element={<RegistrationSuccess />} />
      </Route>

      {/* Error/status pages */}
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="/forbidden" element={<Forbidden />} />

      {/* Main App Routes — /dashboard is the reference homepage */}
      <Route path="/" element={<Home />} />
      <Route path="/home" element={<Home />} />
      <Route path="/dashboard" element={<Home />} />
      <Route path="/citizen" element={<Navigate to="/dashboard" replace />} />
      <Route path="/officer" element={<Navigate to="/dashboard" replace />} />
      <Route path="/admin" element={<Navigate to="/dashboard" replace />} />
      <Route path="/community" element={<Navigate to="/dashboard#community-reports" replace />} />
      <Route path="/guidelines" element={<CivicGuidelinesPage />} />
      <Route path="/how-it-works" element={<HowItWorksPage />} />
      <Route path="/about" element={<AboutPage />} />

      {/* Authenticated routes: complaint reporting & citizen complaint history */}
      <Route element={<ProtectedRoute />}>
        <Route path="/report-issue" element={<ReportIssuePage />} />
        <Route path="/my-reports" element={<MyReportsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}