package com.civicsense.complaint.service.geo;

import com.civicsense.complaint.dto.geo.*;
import com.civicsense.complaint.entity.geo.AdministrativeWard;
import com.civicsense.complaint.entity.geo.AdministrativeZone;
import com.civicsense.complaint.entity.geo.BoundaryProvenance;
import com.civicsense.complaint.repository.AdministrativeWardRepository;
import com.civicsense.complaint.repository.AdministrativeZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrativeGeographyServiceTest {

    @Mock
    private AdministrativeZoneRepository zoneRepository;

    @Mock
    private AdministrativeWardRepository wardRepository;

    @InjectMocks
    private AdministrativeGeographyService geographyService;

    private AdministrativeZone southZone;
    private AdministrativeWard hauzKhasWard;
    private UUID zoneId;
    private UUID wardId;

    @BeforeEach
    void setUp() {
        zoneId = UUID.randomUUID();
        wardId = UUID.randomUUID();

        southZone = new AdministrativeZone("South Delhi Zone", "DL-SOUTH", "Municipal Corporation of Delhi (MCD)", "Delhi NCR");
        southZone.setId(zoneId);

        hauzKhasWard = new AdministrativeWard(
                southZone, "Hauz Khas", "WARD-142-S",
                "Municipal Corporation of Delhi (MCD)", BoundaryProvenance.PROVISIONAL_BOUNDS,
                28.5200, 28.5700, 77.1800, 77.2300,
                28.5494, 77.2001
        );
        hauzKhasWard.setId(wardId);
    }

    @Test
    @DisplayName("Should create zone successfully")
    void testCreateZone_Success() {
        CreateZoneRequest request = new CreateZoneRequest(
                "Central Delhi Zone", "DL-CENTRAL", "Municipal Corporation of Delhi (MCD)", "Delhi NCR", "Official delimitation"
        );

        when(zoneRepository.existsByMunicipalityAndCode("Municipal Corporation of Delhi (MCD)", "DL-CENTRAL")).thenReturn(false);
        when(zoneRepository.save(any(AdministrativeZone.class))).thenAnswer(invocation -> {
            AdministrativeZone z = invocation.getArgument(0);
            z.setId(UUID.randomUUID());
            return z;
        });

        ZoneResponse response = geographyService.createZone(request);

        assertNotNull(response);
        assertEquals("Central Delhi Zone", response.name());
        assertEquals("DL-CENTRAL", response.code());
        assertEquals("Municipal Corporation of Delhi (MCD)", response.municipality());
    }

    @Test
    @DisplayName("Should reject duplicate zone code in same municipality")
    void testCreateZone_DuplicateCode_ThrowsException() {
        CreateZoneRequest request = new CreateZoneRequest(
                "Duplicate South Zone", "DL-SOUTH", "Municipal Corporation of Delhi (MCD)", "Delhi NCR", null
        );

        when(zoneRepository.existsByMunicipalityAndCode("Municipal Corporation of Delhi (MCD)", "DL-SOUTH")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> geographyService.createZone(request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should create ward with valid coordinates and PROVISIONAL_BOUNDS")
    void testCreateWard_Success_WithZone() {
        CreateWardRequest request = new CreateWardRequest(
                zoneId, "Saket", "WARD-143-S", "Municipal Corporation of Delhi (MCD)",
                BoundaryProvenance.PROVISIONAL_BOUNDS, "Test data",
                28.5000, 28.5400, 77.2000, 77.2400, 28.5244, 77.2167
        );

        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(southZone));
        when(wardRepository.existsByMunicipalityAndWardNumber("Municipal Corporation of Delhi (MCD)", "WARD-143-S")).thenReturn(false);
        when(wardRepository.save(any(AdministrativeWard.class))).thenAnswer(invocation -> {
            AdministrativeWard w = invocation.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });

        WardResponse response = geographyService.createWard(request);

        assertNotNull(response);
        assertEquals("Saket", response.name());
        assertEquals("WARD-143-S", response.wardNumber());
        assertEquals(BoundaryProvenance.PROVISIONAL_BOUNDS, response.boundaryProvenance());
        assertEquals(zoneId, response.zoneId());
    }

    @Test
    @DisplayName("Should create direct municipal ward without intermediate zone")
    void testCreateWard_DirectMunicipal_NoZone() {
        CreateWardRequest request = new CreateWardRequest(
                null, "Ghaziabad Ward 1", "GZB-01", "Ghaziabad Nagar Nigam",
                BoundaryProvenance.NO_BOUNDARY, "Official gazette",
                null, null, null, null, null, null
        );

        when(wardRepository.existsByMunicipalityAndWardNumber("Ghaziabad Nagar Nigam", "GZB-01")).thenReturn(false);
        when(wardRepository.save(any(AdministrativeWard.class))).thenAnswer(invocation -> {
            AdministrativeWard w = invocation.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });

        WardResponse response = geographyService.createWard(request);

        assertNotNull(response);
        assertNull(response.zoneId());
        assertEquals("Ghaziabad Nagar Nigam", response.municipality());
        assertEquals(BoundaryProvenance.NO_BOUNDARY, response.boundaryProvenance());
    }

    @Test
    @DisplayName("Should reject fabricated coordinates when provenance is NO_BOUNDARY")
    void testCreateWard_NoBoundary_WithCoordinates_ThrowsException() {
        CreateWardRequest request = new CreateWardRequest(
                null, "Official Ward", "WARD-999", "Municipal Corporation of Delhi (MCD)",
                BoundaryProvenance.NO_BOUNDARY, "Gazette order",
                28.5000, 28.5400, 77.2000, 77.2400, null, null
        );

        when(wardRepository.existsByMunicipalityAndWardNumber("Municipal Corporation of Delhi (MCD)", "WARD-999")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> geographyService.createWard(request));
        assertTrue(ex.getMessage().contains("Fabricated coordinates are forbidden"));
    }

    @Test
    @DisplayName("Should reject ward with invalid coordinates where minLat > maxLat")
    void testCreateWard_InvertedCoordinates_ThrowsException() {
        CreateWardRequest request = new CreateWardRequest(
                null, "Invalid Ward", "WARD-INV", "Municipal Corporation of Delhi (MCD)",
                BoundaryProvenance.PROVISIONAL_BOUNDS, null,
                28.6000, 28.5000, 77.1000, 77.2000, null, null
        );

        when(wardRepository.existsByMunicipalityAndWardNumber("Municipal Corporation of Delhi (MCD)", "WARD-INV")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> geographyService.createWard(request));
        assertTrue(ex.getMessage().contains("minLatitude"));
    }

    @Test
    @DisplayName("Should prevent modifying VERIFIED_BOUNDARY coordinates without forceBoundaryOverwrite flag")
    void testUpdateWard_VerifiedBoundary_WithoutForce_ThrowsException() {
        hauzKhasWard.setBoundaryProvenance(BoundaryProvenance.VERIFIED_BOUNDARY);
        when(wardRepository.findById(wardId)).thenReturn(Optional.of(hauzKhasWard));

        UpdateWardRequest request = new UpdateWardRequest(
                zoneId, "Hauz Khas Modified", "WARD-142-S", "Municipal Corporation of Delhi (MCD)",
                BoundaryProvenance.VERIFIED_BOUNDARY, "Updated", true,
                28.5100, 28.5800, 77.1700, 77.2400, null, null,
                false // forceBoundaryOverwrite = false
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> geographyService.updateWard(wardId, request));
        assertTrue(ex.getMessage().contains("Overwriting or downgrading verified municipal delimitation boundaries requires explicit forceBoundaryOverwrite"));
    }

    @Test
    @DisplayName("Should allow modifying VERIFIED_BOUNDARY coordinates when forceBoundaryOverwrite is true")
    void testUpdateWard_VerifiedBoundary_WithForce_Success() {
        hauzKhasWard.setBoundaryProvenance(BoundaryProvenance.VERIFIED_BOUNDARY);
        when(wardRepository.findById(wardId)).thenReturn(Optional.of(hauzKhasWard));
        when(wardRepository.save(any(AdministrativeWard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateWardRequest request = new UpdateWardRequest(
                zoneId, "Hauz Khas Modified", "WARD-142-S", "Municipal Corporation of Delhi (MCD)",
                BoundaryProvenance.VERIFIED_BOUNDARY, "Updated boundary order", true,
                28.5100, 28.5800, 77.1700, 77.2400, 28.5450, 77.2050,
                true // forceBoundaryOverwrite = true
        );

        WardResponse response = geographyService.updateWard(wardId, request);

        assertNotNull(response);
        assertEquals(28.5100, response.minLatitude());
        assertEquals(28.5800, response.maxLatitude());
        assertEquals("Hauz Khas Modified", response.name());
    }
}

