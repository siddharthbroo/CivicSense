import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { getCommunityFeed, getNearbyCommunityComplaints, supportComplaint, removeSupport } from '../../api/communityApi.js'

import HomeNavbar from '../../components/home/HomeNavbar.jsx'
import HeroSection from '../../components/home/HeroSection.jsx'
import StatsBar from '../../components/home/StatsBar.jsx'
import RecentReportsSection from '../../components/home/RecentReportsSection.jsx'
import FeaturesSection from '../../components/home/FeaturesSection.jsx'
import BottomCtaSection from '../../components/home/BottomCtaSection.jsx'
import HomeFooter from '../../components/home/HomeFooter.jsx'

export default function Home() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [complaints, setComplaints] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [supportingId, setSupportingId] = useState(null)
  const [toastMessage, setToastMessage] = useState('')
  const [userLocation, setUserLocation] = useState(null)
  const [isNearbyOnly, setIsNearbyOnly] = useState(false)

  // Show a temporary feedback message
  const showToast = useCallback((msg) => {
    setToastMessage(msg)
    setTimeout(() => {
      setToastMessage('')
    }, 4000)
  }, [])

  // Fetch complaints from existing Community API (all or nearby based on user consent)
  const loadCommunityFeed = useCallback(async (location = userLocation, nearby = isNearbyOnly) => {
    setIsLoading(true)
    setError('')
    try {
      let data
      if (nearby && location) {
        data = await getNearbyCommunityComplaints({
          latitude: location.latitude,
          longitude: location.longitude,
          radiusKm: 5.0,
        })
      } else {
        data = await getCommunityFeed({ sort: 'RECENT' })
      }
      setComplaints(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load community feed:', err)
      setError(err.message || 'Unable to load community reports from the server.')
    } finally {
      setIsLoading(false)
    }
  }, [userLocation, isNearbyOnly])

  // Request user location only on explicit user click / toggle
  const handleToggleNearby = useCallback(() => {
    if (isNearbyOnly) {
      setIsNearbyOnly(false)
      loadCommunityFeed(userLocation, false)
      return
    }

    if (userLocation) {
      setIsNearbyOnly(true)
      loadCommunityFeed(userLocation, true)
      return
    }

    if (!navigator.geolocation) {
      showToast('Geolocation is not supported by your browser')
      return
    }

    setIsLoading(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const coords = {
          latitude: Number(position.coords.latitude.toFixed(6)),
          longitude: Number(position.coords.longitude.toFixed(6)),
        }
        setUserLocation(coords)
        setIsNearbyOnly(true)
        showToast('Showing community reports within 5km of your area')
        loadCommunityFeed(coords, true)
      },
      (err) => {
        setIsLoading(false)
        console.warn('Location permission denied or unavailable:', err)
        showToast('Location permission denied. Showing all city reports.')
      },
      { timeout: 10000, enableHighAccuracy: false }
    )
  }, [isNearbyOnly, userLocation, loadCommunityFeed, showToast])

  useEffect(() => {
    let ignore = false
    async function fetchFeed() {
      setIsLoading(true)
      setError('')
      try {
        const data = await getCommunityFeed({ sort: 'RECENT' })
        if (!ignore) {
          setComplaints(Array.isArray(data) ? data : [])
        }
      } catch (err) {
        if (!ignore) {
          console.error('Failed to load community feed:', err)
          setError(err.message || 'Unable to load community reports from the server.')
        }
      } finally {
        if (!ignore) {
          setIsLoading(false)
        }
      }
    }
    fetchFeed()
    return () => {
      ignore = true
    }
  }, [])

  // Handle "I'm affected too" support toggle connected directly to backend API
  async function handleSupportToggle(complaint) {
    if (!complaint || !complaint.complaintId) return

    // If unauthenticated, guide user to login
    if (!isAuthenticated) {
      showToast('Please sign in to support this civic report with "I\'m affected too"')
      setTimeout(() => {
        navigate('/login', { state: { from: { pathname: '/dashboard' } } })
      }, 1500)
      return
    }

    const currentSupported = complaint.supportedByCurrentUser
    const targetId = complaint.complaintId
    setSupportingId(targetId)

    // Optimistic UI update
    setComplaints((prev) =>
      prev.map((item) => {
        if (item.complaintId === targetId) {
          const newCount = currentSupported
            ? Math.max(0, (item.supportCount || 0) - 1)
            : (item.supportCount || 0) + 1
          return {
            ...item,
            supportedByCurrentUser: !currentSupported,
            supportCount: newCount,
          }
        }
        return item
      })
    )

    try {
      let res
      if (currentSupported) {
        // DELETE /api/v1/community/complaints/{id}/support
        res = await removeSupport(targetId)
        showToast('Support removed successfully')
      } else {
        // POST /api/v1/community/complaints/{id}/support
        res = await supportComplaint(targetId)
        showToast('Thank you! You marked "I\'m affected too"')
      }

      // Sync with confirmed backend response if returned
      if (res && typeof res.supportCount === 'number') {
        setComplaints((prev) =>
          prev.map((item) =>
            item.complaintId === targetId
              ? {
                  ...item,
                  supportCount: res.supportCount,
                  supportedByCurrentUser: res.supportedByCurrentUser,
                }
              : item
          )
        )
      }
    } catch (err) {
      console.error('Support action error:', err)
      showToast(err.message || 'Unable to update support. Please try again.')
      // Roll back on error from server
      loadCommunityFeed()
    } finally {
      setSupportingId(null)
    }
  }

  // Real-time local sync when comment is created or deleted in detail modal
  const handleCommentCountChange = useCallback((complaintId, countDelta) => {
    setComplaints((prev) =>
      prev.map((item) =>
        item.complaintId === complaintId
          ? {
              ...item,
              commentCount: Math.max(0, (item.commentCount || 0) + countDelta),
            }
          : item
      )
    )
  }, [])

  function handleSearchClick() {
    const el = document.getElementById('community-reports')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  return (
    <div className="min-h-screen bg-slate-25 flex flex-col font-sans text-slate-800 antialiased selection:bg-emerald-100 selection:text-emerald-900">
      {/* Toast Notification Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in fade-in slide-in-from-bottom-5 duration-300">
          <div className="flex items-center gap-3 bg-slate-900 text-white px-5 py-3.5 rounded-2xl shadow-2xl border border-slate-700 text-sm font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-400" />
            <span>{toastMessage}</span>
          </div>
        </div>
      )}

      {/* 1. Header / Navbar */}
      <HomeNavbar onSearchClick={handleSearchClick} />

      {/* Main Content Sections */}
      <main className="flex-1">
        {/* 2. Hero Section with City Background & Floating Card */}
        <HeroSection />

        {/* 3. Floating Statistics Bar */}
        <StatsBar complaints={complaints} />

        {/* 4. Recent Community Reports Section with Complaint Cards */}
        <RecentReportsSection
          complaints={complaints}
          onSupportToggle={handleSupportToggle}
          supportingId={supportingId}
          isLoading={isLoading}
          error={error}
          onRetry={loadCommunityFeed}
          onCommentCountChange={handleCommentCountChange}
          isNearbyOnly={isNearbyOnly}
          userLocation={userLocation}
          onToggleNearby={handleToggleNearby}
        />

        {/* 5. Feature / Benefit Section (Safer Neighborhoods, Cleaner Cities, etc.) */}
        <FeaturesSection />

        {/* 6. Bottom CTA Section ("Be the Change in Your Community") */}
        <BottomCtaSection />
      </main>

      {/* 7. Footer */}
      <HomeFooter />
    </div>
  )
}
