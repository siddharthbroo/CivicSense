package com.civicsense.complaint.service.geo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for validating incident coordinates and enforcing CivicSense's supported service area
 * (Delhi NCR), coordinate sanity, swapped-coordinate detection, and category-aware water-body advisories.
 */
@Service
public class LocationValidationService {

    // Configurable Delhi NCR service area boundaries (defaults cover NCT of Delhi and immediate NCR)
    @Value("${civicsense.service-area.min-latitude:28.20}")
    private double minLatitude = 28.20;

    @Value("${civicsense.service-area.max-latitude:29.00}")
    private double maxLatitude = 29.00;

    @Value("${civicsense.service-area.min-longitude:76.70}")
    private double minLongitude = 76.70;

    @Value("${civicsense.service-area.max-longitude:77.60}")
    private double maxLongitude = 77.60;

    public LocationValidationService() {
    }

    public LocationValidationService(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLatitude = minLat;
        this.maxLatitude = maxLat;
        this.minLongitude = minLon;
        this.maxLongitude = maxLon;
    }

    /**
     * Validates incident coordinates without throwing, returning a structured validation result.
     *
     * @param latitude  Incident latitude
     * @param longitude Incident longitude
     * @param category  Optional civic category (e.g. pothole, streetlight, water)
     * @return LocationValidationResult containing status, warnings, and messages
     */
    public LocationValidationResult validateIncidentLocation(Double latitude, Double longitude, String category) {
        // 1. Missing coordinates
        if (latitude == null || longitude == null) {
            return LocationValidationResult.invalid("Incident coordinates (latitude and longitude) are required.");
        }

        // 2. Non-finite values
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)
                || Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            return LocationValidationResult.invalid("Invalid coordinate numbers provided.");
        }

        // 3. Null Island (0, 0)
        if (Math.abs(latitude) < 0.0001 && Math.abs(longitude) < 0.0001) {
            return LocationValidationResult.invalid(
                    "Coordinates (0,0) represent Null Island and cannot be used as an incident location."
            );
        }

        // 4. Mathematical coordinate range bounds
        if (latitude < -90.0 || latitude > 90.0) {
            // Check if user entered swapped coordinates (e.g. entered longitude in latitude field)
            if (isSwappedDelhiNcr(latitude, longitude)) {
                return LocationValidationResult.swapped(
                        longitude,
                        latitude,
                        String.format(Locale.US,
                                "Latitude (%f) is out of range [-90, 90] and appears swapped with Longitude (%f). Did you mean Latitude %.6f, Longitude %.6f?",
                                latitude, longitude, longitude, latitude)
                );
            }
            return LocationValidationResult.invalid("Latitude must be between -90.0 and 90.0 degrees.");
        }

        if (longitude < -180.0 || longitude > 180.0) {
            return LocationValidationResult.invalid("Longitude must be between -180.0 and 180.0 degrees.");
        }

        // 5. Swapped coordinates detection within standard ranges
        // e.g. Latitude entered as ~77.209 and Longitude entered as ~28.613
        if (isSwappedDelhiNcr(latitude, longitude) && !isWithinServiceArea(latitude, longitude)) {
            return LocationValidationResult.swapped(
                    longitude,
                    latitude,
                    String.format(Locale.US,
                            "Latitude (%.4f) and Longitude (%.4f) appear to be swapped. Did you mean Latitude %.6f, Longitude %.6f?",
                            latitude, longitude, longitude, latitude)
            );
        }

        // 6. CivicSense Supported Service Area (Delhi NCR)
        if (!isWithinServiceArea(latitude, longitude)) {
            return LocationValidationResult.outsideServiceArea(
                    String.format(Locale.US,
                            "Location (%.4f, %.4f) is outside CivicSense's supported service area (Delhi NCR). CivicSense currently only accepts reports within Delhi NCR.",
                            latitude, longitude)
            );
        }

        // 7. Water-body plausibility check (Category-aware advisory)
        String waterBodyName = detectWaterBody(latitude, longitude);
        boolean isWaterBody = (waterBodyName != null);
        boolean waterBodyAdvisory = false;
        String advisoryMessage = null;

        if (isWaterBody && isLandInfrastructureCategory(category)) {
            waterBodyAdvisory = true;
            advisoryMessage = String.format(Locale.US,
                    "This location is in or near a water body (%s). If this issue is on a bridge or elevated flyover, please confirm; otherwise, adjust the pin to the road surface.",
                    waterBodyName);
        }

        return LocationValidationResult.success(waterBodyAdvisory, waterBodyName, advisoryMessage);
    }

    /**
     * Enforces coordinate validation by throwing an IllegalArgumentException if coordinates are invalid
     * or outside the supported service area.
     *
     * @param latitude  Incident latitude
     * @param longitude Incident longitude
     * @param category  Optional civic category
     * @throws IllegalArgumentException on invalid coordinates or out-of-service-area locations
     */
    public void validateOrThrow(Double latitude, Double longitude, String category) {
        LocationValidationResult result = validateIncidentLocation(latitude, longitude, category);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.errorMessage());
        }
    }

    /**
     * Checks if coordinates fall within CivicSense's configured Delhi NCR service area.
     */
    public boolean isWithinServiceArea(double lat, double lon) {
        return lat >= minLatitude && lat <= maxLatitude
                && lon >= minLongitude && lon <= maxLongitude;
    }

    /**
     * Checks if coordinates appear to be swapped (i.e. longitude provided in latitude and vice versa).
     */
    private boolean isSwappedDelhiNcr(double lat, double lon) {
        // If swapped, 'lat' would be longitude (76.70 to 77.60) and 'lon' would be latitude (28.20 to 29.00)
        return lat >= minLongitude && lat <= maxLongitude
                && lon >= minLatitude && lon <= maxLatitude;
    }

    /**
     * Checks if a category is strictly land infrastructure where a direct water-body pin is implausible
     * unless located on an elevated bridge.
     */
    public boolean isLandInfrastructureCategory(String category) {
        if (category == null) return false;
        String cat = category.trim().toLowerCase(Locale.US);
        return cat.contains("pothole") || cat.contains("road")
                || cat.contains("streetlight") || cat.contains("street_light")
                || cat.contains("street light") || cat.contains("footpath")
                || cat.contains("sidewalk");
    }

    /**
     * Detects prominent Delhi NCR water bodies (Yamuna River, Sanjay Lake, Bhalswa Lake, Hauz Khas Lake)
     * using distance to known centerline segments and water centroid buffers.
     *
     * @return Water body name if coordinates fall within water bounds, or null otherwise.
     */
    public String detectWaterBody(double lat, double lon) {
        // 1. Sanjay Lake (East Delhi) ~ 28.6127, 77.3039, radius ~350m
        if (haversineMeters(lat, lon, 28.6127, 77.3039) <= 350.0) {
            return "Sanjay Lake";
        }

        // 2. Bhalswa Lake (North Delhi) ~ 28.7416, 77.1636, radius ~450m
        if (haversineMeters(lat, lon, 28.7416, 77.1636) <= 450.0) {
            return "Bhalswa Lake";
        }

        // 3. Hauz Khas Lake ~ 28.5532, 77.1942, radius ~180m
        if (haversineMeters(lat, lon, 28.5532, 77.1942) <= 180.0) {
            return "Hauz Khas Lake";
        }

        // 4. Yamuna River Corridor segments (North to South through Delhi)
        // Segment A: Wazirabad to Old Railway Bridge
        if (distanceToSegmentMeters(lat, lon, 28.7150, 77.2280, 28.6750, 77.2380) <= 450.0) {
            return "Yamuna River Corridor";
        }
        // Segment B: Old Railway Bridge to ITO / Geeta Colony
        if (distanceToSegmentMeters(lat, lon, 28.6750, 77.2380, 28.6280, 77.2500) <= 500.0) {
            return "Yamuna River Corridor";
        }
        // Segment C: ITO to Barapullah / Nizamuddin
        if (distanceToSegmentMeters(lat, lon, 28.6280, 77.2500, 28.5850, 77.2680) <= 500.0) {
            return "Yamuna River Corridor";
        }
        // Segment D: Nizamuddin to Kalindi Kunj / Okhla
        if (distanceToSegmentMeters(lat, lon, 28.5850, 77.2680, 28.5400, 77.3120) <= 500.0) {
            return "Yamuna River Corridor";
        }

        return null;
    }

    private double distanceToSegmentMeters(double pLat, double pLon, double aLat, double aLon, double bLat, double bLon) {
        double dLat = bLat - aLat;
        double dLon = bLon - aLon;
        double lenSq = (dLat * dLat) + (dLon * dLon);
        if (lenSq == 0.0) {
            return haversineMeters(pLat, pLon, aLat, aLon);
        }

        double t = Math.max(0.0, Math.min(1.0, ((pLat - aLat) * dLat + (pLon - aLon) * dLon) / lenSq));
        double projLat = aLat + t * dLat;
        double projLon = aLon + t * dLon;
        return haversineMeters(pLat, pLon, projLat, projLon);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }
}
