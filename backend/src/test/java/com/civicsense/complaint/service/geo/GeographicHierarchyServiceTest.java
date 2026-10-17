package com.civicsense.complaint.service.geo;

import com.civicsense.complaint.entity.geo.AdministrativeWard;
import com.civicsense.complaint.entity.geo.AdministrativeZone;
import com.civicsense.complaint.repository.AdministrativeWardRepository;
import com.civicsense.complaint.repository.AdministrativeZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeographicHierarchyServiceTest {

    @Mock
    private AdministrativeZoneRepository zoneRepository;

    @Mock
    private AdministrativeWardRepository wardRepository;

    @InjectMocks
    private GeographicHierarchyService geographicHierarchyService;

    private AdministrativeZone southZone;
    private AdministrativeWard hauzKhasWard;

    @BeforeEach
    void setUp() {
        southZone = new AdministrativeZone("South Delhi Zone", "DL-SOUTH");
        hauzKhasWard = new AdministrativeWard(
                southZone, "Hauz Khas", "WARD-142-S",
                28.5200, 28.5700, 77.1800, 77.2300,
                28.5494, 77.2001
        );
    }

    @Test
    @DisplayName("Should resolve confirmed map pin to matching ward")
    void testResolveWard_Success() {
        // Hauz Khas coords: 28.5494, 77.2001
        when(wardRepository.findWardsContainingPoint(28.5494, 77.2001))
                .thenReturn(List.of(hauzKhasWard));

        Optional<AdministrativeWard> resolved = geographicHierarchyService.resolveWard(28.5494, 77.2001);

        assertTrue(resolved.isPresent());
        assertEquals("Hauz Khas", resolved.get().getName());
        assertEquals("WARD-142-S", resolved.get().getWardNumber());
        assertEquals("South Delhi Zone", resolved.get().getZone().getName());
    }

    @Test
    @DisplayName("Should return empty when coordinates do not fall into any ward")
    void testResolveWard_OutOfBounds() {
        // Point far outside Delhi NCR
        when(wardRepository.findWardsContainingPoint(19.0760, 72.8777))
                .thenReturn(Collections.emptyList());

        Optional<AdministrativeWard> resolved = geographicHierarchyService.resolveWard(19.0760, 72.8777);

        assertTrue(resolved.isEmpty());
    }

    @Test
    @DisplayName("Should handle null coordinates gracefully")
    void testResolveWard_NullCoordinates() {
        Optional<AdministrativeWard> resolved = geographicHierarchyService.resolveWard(null, null);

        assertTrue(resolved.isEmpty());
        verifyNoInteractions(wardRepository);
    }

    @Test
    @DisplayName("Should pick closest centroid when bounding boxes overlap")
    void testResolveWard_OverlappingBoxes_PicksClosestCentroid() {
        AdministrativeWard saketWard = new AdministrativeWard(
                southZone, "Saket", "WARD-143-S",
                28.5100, 28.5500, 77.1900, 77.2300,
                28.5244, 77.2167
        );

        // Pin is closer to Hauz Khas centroid (28.5494, 77.2001) than Saket centroid (28.5244, 77.2167)
        double pinLat = 28.5480;
        double pinLon = 28.2010;

        when(wardRepository.findWardsContainingPoint(pinLat, pinLon))
                .thenReturn(List.of(hauzKhasWard, saketWard));

        Optional<AdministrativeWard> resolved = geographicHierarchyService.resolveWard(pinLat, pinLon);

        assertTrue(resolved.isPresent());
        assertEquals("Hauz Khas", resolved.get().getName());
    }
}