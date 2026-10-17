import { useState, useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  validateCoordinates,
  fetchReverseGeocode,
} from '../../utils/locationValidation.js'

// Default fallback coordinates: Delhi NCR (Central Delhi)
const DEFAULT_FALLBACK_LAT = 28.613939
const DEFAULT_FALLBACK_LNG = 77.209021

// Crisp SVG map marker matching CivicSense teal brand design
const customMarkerIcon = L.divIcon({
  className: 'custom-map-pin',
  html: `
    <div style="position: relative; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: grab;">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 3px 6px rgba(11,21,38,0.35));">
        <path d="M12 2C8.13 2 5 5.13 5 9C5 14.25 12 22 12 22C12 22 19 14.25 19 9C19 5.13 15.87 2 12 2Z" fill="#0D9488" stroke="#FFFFFF" stroke-width="1.8"/>
        <circle cx="12" cy="9" r="3.2" fill="#FFFFFF"/>
      </svg>
    </div>
  `,
  iconSize: [36, 36],
  iconAnchor: [18, 36],
  popupAnchor: [0, -36],
})

// Resolve starting coordinates in order of priority:
// 1. Current form latitude/longitude if valid
// 2. Suggested device GPS if available
// 3. Delhi NCR default fallback
function resolveInitialCoords(initialLatitude, initialLongitude, suggestedLatitude, suggestedLongitude) {
  const initLat = parseFloat(initialLatitude)
  const initLng = parseFloat(initialLongitude)
  if (!isNaN(initLat) && !isNaN(initLng) && initLat >= -90 && initLat <= 90 && initLng >= -180 && initLng <= 180) {
    return { latitude: Number(initLat.toFixed(6)), longitude: Number(initLng.toFixed(6)) }
  }

  const suggLat = parseFloat(suggestedLatitude)
  const suggLng = parseFloat(suggestedLongitude)
  if (!isNaN(suggLat) && !isNaN(suggLng) && suggLat >= -90 && suggLat <= 90 && suggLng >= -180 && suggLng <= 180) {
    return { latitude: Number(suggLat.toFixed(6)), longitude: Number(suggLng.toFixed(6)) }
  }

  return { latitude: DEFAULT_FALLBACK_LAT, longitude: DEFAULT_FALLBACK_LNG }
}

/**
 * Interactive Leaflet location picker modal.
 * Allows citizens to click or drag the marker to pinpoint the exact incident location,
 * search for landmarks via OpenStreetMap Nominatim, and explicitly confirm coordinates.
 */
export default function LocationPickerModal({
  isOpen,
  onClose,
  onConfirm,
  initialLatitude,
  initialLongitude,
  suggestedLatitude,
  suggestedLongitude,
  category,
}) {
  const mapContainerRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRef = useRef(null)

  // Local state for the marker inside the picker modal.
  // The parent form's coordinates are NOT touched until the citizen explicitly clicks "Confirm Location".
  const [selectedCoords, setSelectedCoords] = useState(() =>
    resolveInitialCoords(initialLatitude, initialLongitude, suggestedLatitude, suggestedLongitude)
  )

  // Live validation result for the currently selected pin (computed directly, no cascading renders)
  const locationValidation = validateCoordinates(selectedCoords.latitude, selectedCoords.longitude, category)

  // Reverse-geocoded address for the selected pin (display only)
  const [reverseAddress, setReverseAddress] = useState(null)

  // Search state
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [isSearching, setIsSearching] = useState(false)
  const [searchError, setSearchError] = useState('')

  // Geolocation detection state within modal
  const [isLocating, setIsLocating] = useState(false)

  // Initialize Leaflet map instance when modal opens
  useEffect(() => {
    if (!isOpen || !mapContainerRef.current) return

    const { latitude: startLat, longitude: startLng } = resolveInitialCoords(
      initialLatitude,
      initialLongitude,
      suggestedLatitude,
      suggestedLongitude
    )

    // Initialize map
    const map = L.map(mapContainerRef.current, {
      center: [startLat, startLng],
      zoom: 15,
      zoomControl: false,
    })
    mapInstanceRef.current = map

    // Place zoom controls at bottom right
    L.control.zoom({ position: 'bottomright' }).addTo(map)

    // Add free OpenStreetMap tile layer (no API key required)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors',
    }).addTo(map)

    // Add movable marker
    const marker = L.marker([startLat, startLng], {
      draggable: true,
      icon: customMarkerIcon,
    }).addTo(map)
    markerRef.current = marker

    // Marker drag handler
    marker.on('dragend', (e) => {
      const pos = e.target.getLatLng()
      setSelectedCoords({
        latitude: Number(pos.lat.toFixed(6)),
        longitude: Number(pos.lng.toFixed(6)),
      })
    })

    // Map click handler: moves marker to clicked spot
    map.on('click', (e) => {
      marker.setLatLng(e.latlng)
      setSelectedCoords({
        latitude: Number(e.latlng.lat.toFixed(6)),
        longitude: Number(e.latlng.lng.toFixed(6)),
      })
    })

    // Invalidate size to ensure full tile rendering after container mounts
    const timer = setTimeout(() => {
      map.invalidateSize()
    }, 150)

    // Cleanup on unmount or when modal closes
    return () => {
      clearTimeout(timer)
      map.remove()
      mapInstanceRef.current = null
      markerRef.current = null
    }
  }, [isOpen, initialLatitude, initialLongitude, suggestedLatitude, suggestedLongitude])

  // Handle escape key to close without saving
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && isOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  // Fetch reverse-geocoded address for the pin (only when pin is in service area)
  useEffect(() => {
    let cancelled = false

    if (!locationValidation.isValid) {
      return
    }

    fetchReverseGeocode(selectedCoords.latitude, selectedCoords.longitude)
      .then((addr) => {
        if (!cancelled) setReverseAddress(addr)
      })
      .catch(() => {
        if (!cancelled) setReverseAddress(null)
      })

    return () => {
      cancelled = true
    }
  }, [selectedCoords.latitude, selectedCoords.longitude, locationValidation.isValid])

  // Search location using OpenStreetMap Nominatim (free, no API key required)
  async function handleSearch(e) {
    e?.preventDefault()
    const query = searchQuery.trim()
    if (!query) return

    setIsSearching(true)
    setSearchError('')
    setSearchResults([])

    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
          query
        )}&limit=5`,
        {
          headers: {
            'Accept-Language': 'en',
          },
        }
      )
      if (!response.ok) throw new Error('Search service unavailable')
      const data = await response.json()
      if (Array.isArray(data) && data.length > 0) {
        setSearchResults(data)
      } else {
        setSearchError('No matching places found. Try another landmark or area name.')
      }
    } catch {
      setSearchError('Search is temporarily unavailable. You can click anywhere on the map to place the pin.')
    } finally {
      setIsSearching(false)
    }
  }

  // Apply search result
  function handleSelectResult(item) {
    const lat = Number(parseFloat(item.lat).toFixed(6))
    const lng = Number(parseFloat(item.lon).toFixed(6))

    setSelectedCoords({ latitude: lat, longitude: lng })
    setSearchResults([])
    setSearchQuery(item.display_name?.split(',')[0] || searchQuery)

    if (markerRef.current) {
      markerRef.current.setLatLng([lat, lng])
    }
    if (mapInstanceRef.current) {
      mapInstanceRef.current.setView([lat, lng], 16)
    }
  }

  // Center on user's current GPS location inside the picker (suggestion only)
  function handleCenterOnGps() {
    if (!navigator.geolocation) {
      setSearchError('Geolocation is not supported by your browser.')
      return
    }

    setIsLocating(true)
    setSearchError('')

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocating(false)
        const lat = Number(pos.coords.latitude.toFixed(6))
        const lng = Number(pos.coords.longitude.toFixed(6))
        setSelectedCoords({ latitude: lat, longitude: lng })

        if (markerRef.current) {
          markerRef.current.setLatLng([lat, lng])
        }
        if (mapInstanceRef.current) {
          mapInstanceRef.current.setView([lat, lng], 16)
        }
      },
      () => {
        setIsLocating(false)
        setSearchError('Could not retrieve current location. Please pick location manually on the map.')
      },
      { enableHighAccuracy: true, timeout: 10000 }
    )
  }

  // Confirm action: explicitly apply chosen coordinates to parent form
  function handleConfirm() {
    onConfirm({
      latitude: selectedCoords.latitude,
      longitude: selectedCoords.longitude,
    })
  }

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center p-3 sm:p-6 bg-slate-950/75 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="map-picker-title"
    >
      <div
        className="relative w-full max-w-3xl h-[90vh] sm:h-[620px] bg-white rounded-2xl sm:rounded-3xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-4 sm:px-6 py-3.5 border-b border-slate-200 bg-slate-50/80 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-600 text-white shadow-xs">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6" />
                <line x1="8" y1="2" x2="8" y2="18" />
                <line x1="16" y1="6" x2="16" y2="22" />
              </svg>
            </div>
            <div>
              <h3 id="map-picker-title" className="text-sm sm:text-base font-bold text-navy-900 leading-snug">
                Set Incident Location on Map
              </h3>
              <p className="text-[11px] text-slate-500 leading-none mt-0.5">
                Drag the pin or click on the map to pinpoint the exact location.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close map picker"
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors cursor-pointer"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Search & Action Bar */}
        <div className="px-4 sm:px-6 py-2.5 border-b border-slate-200 bg-white shrink-0 relative z-10">
          <form onSubmit={handleSearch} className="flex items-center gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Search landmark, street or sector (e.g. Connaught Place, Rohini Sector 9)..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:border-teal-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-teal-500 transition-all placeholder:text-slate-400"
              />
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </div>

            <button
              type="submit"
              disabled={isSearching || !searchQuery.trim()}
              className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-50 transition-colors shrink-0 cursor-pointer"
            >
              {isSearching ? 'Searching…' : 'Search'}
            </button>

            <button
              type="button"
              onClick={handleCenterOnGps}
              disabled={isLocating}
              title="Center map on current device GPS"
              className="px-2.5 py-1.5 text-xs font-semibold rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 transition-colors flex items-center gap-1.5 shrink-0 cursor-pointer"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={isLocating ? 'animate-spin text-teal-600' : 'text-slate-500'}>
                <circle cx="12" cy="12" r="10" />
                <circle cx="12" cy="12" r="3" />
                <line x1="12" y1="2" x2="12" y2="6" />
                <line x1="12" y1="18" x2="12" y2="22" />
                <line x1="2" y1="12" x2="6" y2="12" />
                <line x1="18" y1="12" x2="22" y2="12" />
              </svg>
              <span className="hidden sm:inline">{isLocating ? 'Locating…' : 'My GPS'}</span>
            </button>
          </form>

          {/* Search Error Message */}
          {searchError && (
            <p className="mt-1.5 text-[11px] text-amber-700 font-medium bg-amber-50 px-2.5 py-1 rounded border border-amber-200">
              {searchError}
            </p>
          )}

          {/* Search Results Dropdown */}
          {searchResults.length > 0 && (
            <div className="absolute left-4 right-4 sm:left-6 sm:right-6 top-full mt-1 bg-white rounded-xl shadow-xl border border-slate-200 overflow-hidden divide-y divide-slate-100 max-h-48 overflow-y-auto z-20">
              {searchResults.map((item, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => handleSelectResult(item)}
                  className="w-full text-left px-3.5 py-2 text-xs hover:bg-teal-50/70 transition-colors flex items-start gap-2 cursor-pointer"
                >
                  <span className="text-teal-600 mt-0.5">📍</span>
                  <span className="text-slate-800 line-clamp-1">{item.display_name}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Map View Area */}
        <div className="flex-1 w-full relative isolate overflow-hidden bg-slate-100">
          <div ref={mapContainerRef} className="w-full h-full" />

          {/* Floating Instructions Helper on Map */}
          <div className="absolute top-3 left-3 z-[400] pointer-events-none">
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-navy-900/80 backdrop-blur-xs text-white text-[11px] font-medium shadow-md">
              <span className="w-1.5 h-1.5 rounded-full bg-teal-400 animate-pulse" />
              Click anywhere on map or drag pin to position
            </span>
          </div>
        </div>

        {/* Modal Footer with Coordinates & Confirmation Actions */}
        <div className="px-4 sm:px-6 pt-3 pb-3.5 border-t border-slate-200 bg-slate-50/90 flex flex-col gap-2.5 shrink-0">

          {/* Validation banners — shown above the pin coords row */}

          {/* Swapped-coordinates suggestion */}
          {!locationValidation.isValid && locationValidation.isSwapped && (
            <div className="flex items-start gap-2 bg-amber-50 border border-amber-300 rounded-xl px-3 py-2 text-xs text-amber-800">
              <svg className="mt-0.5 shrink-0 text-amber-500" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <span className="flex-1">
                Coordinates appear <strong>swapped</strong>.{' '}
                Did you mean <span className="font-mono">{locationValidation.suggestedCoords?.latitude}, {locationValidation.suggestedCoords?.longitude}</span>?
              </span>
              <button
                type="button"
                onClick={() => {
                  const { latitude, longitude } = locationValidation.suggestedCoords
                  setSelectedCoords({ latitude, longitude })
                  if (markerRef.current) markerRef.current.setLatLng([latitude, longitude])
                  if (mapInstanceRef.current) mapInstanceRef.current.setView([latitude, longitude], 15)
                }}
                className="shrink-0 px-2.5 py-1 rounded-lg bg-amber-200 hover:bg-amber-300 text-amber-900 font-semibold transition-colors cursor-pointer"
              >
                Use corrected
              </button>
            </div>
          )}

          {/* Outside service area error */}
          {!locationValidation.isValid && locationValidation.isOutsideServiceArea && (
            <div className="flex items-center gap-2 bg-red-50 border border-red-300 rounded-xl px-3 py-2 text-xs text-red-800">
              <svg className="shrink-0 text-red-500" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
              <span>This location is <strong>outside CivicSense's Delhi NCR service area</strong>. Please move the pin inside Delhi NCR.</span>
            </div>
          )}

          {/* Generic invalid error (null island, NaN, range, etc.) */}
          {!locationValidation.isValid && !locationValidation.isSwapped && !locationValidation.isOutsideServiceArea && (
            <div className="flex items-center gap-2 bg-red-50 border border-red-300 rounded-xl px-3 py-2 text-xs text-red-800">
              <svg className="shrink-0 text-red-500" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
              <span>{locationValidation.error}</span>
            </div>
          )}

          {/* Water-body advisory (amber — never blocks confirmation) */}
          {locationValidation.isValid && locationValidation.waterBodyWarning && (
            <div className="flex items-start gap-2 bg-amber-50 border border-amber-300 rounded-xl px-3 py-2 text-xs text-amber-800">
              <svg className="mt-0.5 shrink-0 text-amber-500" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              <span>{locationValidation.advisory}</span>
            </div>
          )}

          {/* Selected Pin row + action buttons */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2">

            {/* Selected Pin Details + reverse address */}
            <div className="flex flex-col gap-0.5 text-xs">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-slate-600">Selected Pin:</span>
                <span className="font-mono font-bold text-navy-900 bg-white px-2.5 py-1 rounded-md border border-slate-200 text-xs">
                  {selectedCoords.latitude}, {selectedCoords.longitude}
                </span>
              </div>
              {reverseAddress && (
                <span className="text-[11px] text-slate-500 truncate max-w-xs" title={reverseAddress}>
                  📍 {reverseAddress.split(',').slice(0, 3).join(', ')}
                </span>
              )}
            </div>

            {/* Confirm & Cancel Buttons */}
            <div className="flex items-center gap-2 justify-end shrink-0">
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary px-4 py-2 text-xs font-semibold rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                disabled={!locationValidation.isValid}
                className="btn-primary px-5 py-2 text-xs font-bold rounded-xl bg-teal-600 hover:bg-teal-700 disabled:opacity-50 disabled:cursor-not-allowed text-white shadow-sm flex items-center gap-1.5 transition-all"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span>Confirm Location</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  )
}
