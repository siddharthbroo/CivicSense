/**
 * CivicSense Location Validation & Integrity Utilities
 * Enforces coordinate mathematical validity, CivicSense Delhi NCR service area bounds,
 * swapped-coordinate detection with suggestions, and category-aware water-body advisories.
 */

// Authoritative Delhi NCR supported service area boundary
export const DELHI_NCR_BOUNDS = {
  minLat: 28.20,
  maxLat: 29.00,
  minLon: 76.70,
  maxLon: 77.60,
}

// Prominent water bodies within Delhi NCR
const WATER_BODIES = [
  { name: 'Sanjay Lake', lat: 28.6127, lon: 77.3039, radiusMeters: 350 },
  { name: 'Bhalswa Lake', lat: 28.7416, lon: 77.1636, radiusMeters: 450 },
  { name: 'Hauz Khas Lake', lat: 28.5532, lon: 77.1942, radiusMeters: 180 },
]

// Yamuna River Corridor segments (North to South through Delhi)
const YAMUNA_SEGMENTS = [
  { a: [28.7150, 77.2280], b: [28.6750, 77.2380], bufferMeters: 450 }, // Wazirabad to Old Railway Bridge
  { a: [28.6750, 77.2380], b: [28.6280, 77.2500], bufferMeters: 500 }, // Old Railway Bridge to ITO
  { a: [28.6280, 77.2500], b: [28.5850, 77.2680], bufferMeters: 500 }, // ITO to Barapullah / Nizamuddin
  { a: [28.5850, 77.2680], b: [28.5400, 77.3120], bufferMeters: 500 }, // Nizamuddin to Kalindi Kunj
]

/**
 * Calculates Haversine distance in meters between two lat/lon points.
 */
export function haversineMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000 // Earth radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

/**
 * Calculates perpendicular distance from a point to a line segment in meters.
 */
function distanceToSegmentMeters(pLat, pLon, aLat, aLon, bLat, bLon) {
  const dLat = bLat - aLat
  const dLon = bLon - aLon
  const lenSq = dLat * dLat + dLon * dLon
  if (lenSq === 0) return haversineMeters(pLat, pLon, aLat, aLon)

  const t = Math.max(0, Math.min(1, ((pLat - aLat) * dLat + (pLon - aLon) * dLon) / lenSq))
  const projLat = aLat + t * dLat
  const projLon = aLon + t * dLon
  return haversineMeters(pLat, pLon, projLat, projLon)
}

/**
 * Checks if coordinates fall within known Delhi NCR water bodies.
 */
export function detectWaterBody(lat, lon) {
  for (const lake of WATER_BODIES) {
    if (haversineMeters(lat, lon, lake.lat, lake.lon) <= lake.radiusMeters) {
      return lake.name
    }
  }

  for (const seg of YAMUNA_SEGMENTS) {
    if (distanceToSegmentMeters(lat, lon, seg.a[0], seg.a[1], seg.b[0], seg.b[1]) <= seg.bufferMeters) {
      return 'Yamuna River Corridor'
    }
  }

  return null
}

/**
 * Checks if a category is strictly land infrastructure where a direct water-body pin is implausible
 * unless located on an elevated bridge.
 */
export function isLandInfrastructureCategory(category) {
  if (!category) return false
  const cat = String(category).toLowerCase()
  return (
    cat.includes('pothole') ||
    cat.includes('road') ||
    cat.includes('streetlight') ||
    cat.includes('street light') ||
    cat.includes('footpath') ||
    cat.includes('sidewalk')
  )
}

/**
 * Checks whether coordinates fall within CivicSense's configured Delhi NCR service area.
 */
export function isWithinDelhiNcr(lat, lon) {
  return (
    lat >= DELHI_NCR_BOUNDS.minLat &&
    lat <= DELHI_NCR_BOUNDS.maxLat &&
    lon >= DELHI_NCR_BOUNDS.minLon &&
    lon <= DELHI_NCR_BOUNDS.maxLon
  )
}

/**
 * Checks if coordinates appear to be swapped (latitude entered as longitude and vice versa).
 */
export function isSwappedDelhiNcr(lat, lon) {
  // If swapped, 'lat' would be in lon range (76.70 to 77.60) and 'lon' would be in lat range (28.20 to 29.00)
  return (
    lat >= DELHI_NCR_BOUNDS.minLon &&
    lat <= DELHI_NCR_BOUNDS.maxLon &&
    lon >= DELHI_NCR_BOUNDS.minLat &&
    lon <= DELHI_NCR_BOUNDS.maxLat
  )
}

/**
 * Comprehensive client-side coordinate and service area validation.
 *
 * @param {number|string} latitude
 * @param {number|string} longitude
 * @param {string} [category]
 * @returns {object} Validation result
 */
export function validateCoordinates(latitude, longitude, category = null) {
  if (latitude === '' || latitude === null || latitude === undefined ||
      longitude === '' || longitude === null || longitude === undefined) {
    return {
      isValid: false,
      error: 'Incident coordinates are required.',
      isOutsideServiceArea: false,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  const lat = typeof latitude === 'number' ? latitude : parseFloat(latitude)
  const lng = typeof longitude === 'number' ? longitude : parseFloat(longitude)

  if (isNaN(lat) || isNaN(lng) || !isFinite(lat) || !isFinite(lng)) {
    return {
      isValid: false,
      error: 'Please enter valid numerical latitude and longitude.',
      isOutsideServiceArea: false,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  // Null Island (0, 0)
  if (Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001) {
    return {
      isValid: false,
      error: 'Coordinates (0,0) represent Null Island and cannot be used as an incident location.',
      isOutsideServiceArea: false,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  // Mathematical bounds check
  if (lat < -90 || lat > 90) {
    if (isSwappedDelhiNcr(lat, lng)) {
      return {
        isValid: false,
        error: `Latitude (${lat}) is out of range [-90, 90] and appears swapped with Longitude. Did you mean ${lng.toFixed(6)}, ${lat.toFixed(6)}?`,
        isOutsideServiceArea: false,
        isSwapped: true,
        suggestedCoords: { latitude: lng, longitude: lat },
        isWaterBody: false,
        waterBodyName: null,
        waterBodyWarning: false,
        advisory: null,
      }
    }
    return {
      isValid: false,
      error: 'Latitude must be between -90.0 and 90.0 degrees.',
      isOutsideServiceArea: false,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  if (lng < -180 || lng > 180) {
    return {
      isValid: false,
      error: 'Longitude must be between -180.0 and 180.0 degrees.',
      isOutsideServiceArea: false,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  // Swapped coordinates detection within valid range
  if (isSwappedDelhiNcr(lat, lng) && !isWithinDelhiNcr(lat, lng)) {
    return {
      isValid: false,
      error: `Latitude (${lat}) and Longitude (${lng}) appear to be swapped! Did you mean ${lng.toFixed(6)}, ${lat.toFixed(6)}?`,
      isOutsideServiceArea: false,
      isSwapped: true,
      suggestedCoords: { latitude: lng, longitude: lat },
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  // CivicSense Supported Service Area (Delhi NCR)
  if (!isWithinDelhiNcr(lat, lng)) {
    return {
      isValid: false,
      error: `Location (${lat.toFixed(4)}, ${lng.toFixed(4)}) is outside CivicSense's supported service area (Delhi NCR). Please select a location within Delhi NCR.`,
      isOutsideServiceArea: true,
      isSwapped: false,
      suggestedCoords: null,
      isWaterBody: false,
      waterBodyName: null,
      waterBodyWarning: false,
      advisory: null,
    }
  }

  // Water-body check
  const waterBodyName = detectWaterBody(lat, lng)
  const isWaterBody = Boolean(waterBodyName)
  const isLand = isLandInfrastructureCategory(category)
  const waterBodyWarning = isWaterBody && isLand
  const advisory = waterBodyWarning
    ? `⚠️ Water-body notice: Pin is located in or near ${waterBodyName}. If this issue is on a bridge or elevated flyover, please confirm below; otherwise, adjust the pin to the road.`
    : null

  return {
    isValid: true,
    error: null,
    isOutsideServiceArea: false,
    isSwapped: false,
    suggestedCoords: null,
    isWaterBody,
    waterBodyName,
    waterBodyWarning,
    advisory,
  }
}

/**
 * Free OpenStreetMap Nominatim reverse geocoder with simple in-memory caching.
 */
const reverseGeocodeCache = new Map()

export async function fetchReverseGeocode(lat, lon) {
  const key = `${lat.toFixed(5)},${lon.toFixed(5)}`
  if (reverseGeocodeCache.has(key)) {
    return reverseGeocodeCache.get(key)
  }

  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1`
    const res = await fetch(url, {
      headers: {
        'Accept-Language': 'en',
      },
    })
    if (!res.ok) return null
    const data = await res.json()
    const address = data.display_name || null
    if (address) {
      reverseGeocodeCache.set(key, address)
    }
    return address
  } catch {
    return null
  }
}

