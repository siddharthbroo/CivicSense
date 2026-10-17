package com.civicsense.complaint.service.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationValidationServiceTest {

    private LocationValidationService service;

    @BeforeEach
    void setUp() {
        service = new LocationValidationService(28.20, 29.00, 76.70, 77.60);
    }

    @Test
    @DisplayName("Valid coordinates in Delhi NCR succeed")
    void testValidDelhiNcrCoordinates() {
        // Connaught Place (Central Delhi)
        LocationValidationResult cp = service.validateIncidentLocation(28.6315, 77.2167, "pothole");
        assertTrue(cp.valid());
        assertTrue(cp.withinServiceArea());
        assertFalse(cp.swappedSuggested());
        assertNull(cp.errorMessage());

        // Hauz Khas (South Delhi)
        LocationValidationResult hk = service.validateIncidentLocation(28.5494, 77.2001, null);
        assertTrue(hk.valid());
        assertTrue(hk.withinServiceArea());

        // Rohini (North Delhi)
        LocationValidationResult rohini = service.validateIncidentLocation(28.7142, 77.1126, "garbage");
        assertTrue(rohini.valid());
        assertTrue(rohini.withinServiceArea());
    }

    @Test
    @DisplayName("Null or non-numeric coordinates are rejected")
    void testNullAndNonNumericCoordinates() {
        LocationValidationResult nullLat = service.validateIncidentLocation(null, 77.2167, null);
        assertFalse(nullLat.valid());
        assertNotNull(nullLat.errorMessage());

        LocationValidationResult nullLon = service.validateIncidentLocation(28.6315, null, null);
        assertFalse(nullLon.valid());

        LocationValidationResult nan = service.validateIncidentLocation(Double.NaN, 77.2167, null);
        assertFalse(nan.valid());

        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow(null, 77.2167, null));
    }

    @Test
    @DisplayName("(0,0) Null Island is rejected as invalid incident location")
    void testNullIslandRejected() {
        LocationValidationResult nullIsland = service.validateIncidentLocation(0.0, 0.0, "pothole");
        assertFalse(nullIsland.valid());
        assertTrue(nullIsland.errorMessage().contains("Null Island"));

        assertThrows(IllegalArgumentException.class, () -> service.validateOrThrow(0.0, 0.0, "pothole"));
    }

    @Test
    @DisplayName("Out-of-range coordinates are rejected")
    void testOutOfRangeCoordinates() {
        LocationValidationResult invalidLat = service.validateIncidentLocation(95.0, 77.2167, null);
        assertFalse(invalidLat.valid());
        assertTrue(invalidLat.errorMessage().contains("Latitude must be between"));

        LocationValidationResult invalidLon = service.validateIncidentLocation(28.6315, 195.0, null);
        assertFalse(invalidLon.valid());
        assertTrue(invalidLon.errorMessage().contains("Longitude must be between"));
    }

    @Test
    @DisplayName("Swapped coordinates are detected and helpful correction is suggested")
    void testSwappedCoordinatesDetected() {
        // User entered: latitude = 77.2090, longitude = 28.6139 (Connaught Place inverted)
        LocationValidationResult swapped = service.validateIncidentLocation(77.2090, 28.6139, "pothole");
        assertFalse(swapped.valid());
        assertTrue(swapped.swappedSuggested());
        assertEquals(28.6139, swapped.suggestedLatitude());
        assertEquals(77.2090, swapped.suggestedLongitude());
        assertTrue(swapped.errorMessage().contains("swapped"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.validateOrThrow(77.2090, 28.6139, "pothole"));
        assertTrue(ex.getMessage().contains("swapped"));
    }

    @Test
    @DisplayName("Valid coordinates outside Delhi NCR are distinguished and rejected as outside service area")
    void testOutsideServiceArea() {
        // Mumbai coordinates (valid on Earth, but outside Delhi NCR)
        LocationValidationResult mumbai = service.validateIncidentLocation(18.9220, 72.8347, "pothole");
        assertFalse(mumbai.valid());
        assertFalse(mumbai.withinServiceArea());
        assertFalse(mumbai.swappedSuggested());
        assertTrue(mumbai.errorMessage().contains("outside CivicSense's supported service area"));

        // New York coordinates
        LocationValidationResult ny = service.validateIncidentLocation(40.7128, -74.0060, "pothole");
        assertFalse(ny.valid());
        assertFalse(ny.withinServiceArea());

        assertThrows(IllegalArgumentException.class,
                () -> service.validateOrThrow(18.9220, 72.8347, "pothole"));
    }

    @Test
    @DisplayName("Water-body detection produces advisory for road/land infrastructure without rejecting")
    void testWaterBodyCategoryAwareAdvisory() {
        // Location in Yamuna river near Old Railway Bridge: 28.6650, 77.2450
        LocationValidationResult roadInWater = service.validateIncidentLocation(28.6650, 77.2450, "pothole");
        assertTrue(roadInWater.valid(), "Water-body location must NOT be rejected automatically");
        assertTrue(roadInWater.waterBodyWarning());
        assertEquals("Yamuna River Corridor", roadInWater.waterBodyName());
        assertNotNull(roadInWater.advisoryMessage());
        assertTrue(roadInWater.advisoryMessage().contains("bridge"));

        // Same location for water/sewage or garbage issue: no blocking advisory
        LocationValidationResult waterInWater = service.validateIncidentLocation(28.6650, 77.2450, "water");
        assertTrue(waterInWater.valid());
        assertFalse(waterInWater.waterBodyWarning());

        // Sanjay Lake in East Delhi: 28.6127, 77.3039
        LocationValidationResult lakePothole = service.validateIncidentLocation(28.6127, 77.3039, "streetlight");
        assertTrue(lakePothole.valid());
        assertTrue(lakePothole.waterBodyWarning());
        assertEquals("Sanjay Lake", lakePothole.waterBodyName());
    }

    @Test
    @DisplayName("Unresolved ward within Delhi NCR bounds is still accepted as supported service area")
    void testUnresolvedWardStillSupported() {
        // Point in Delhi NCR between wards (e.g. 28.5800, 77.1500)
        LocationValidationResult result = service.validateIncidentLocation(28.5800, 77.1500, "garbage");
        assertTrue(result.valid());
        assertTrue(result.withinServiceArea());
    }
}

