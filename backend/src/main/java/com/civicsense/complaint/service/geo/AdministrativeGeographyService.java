package com.civicsense.complaint.service.geo;

import com.civicsense.complaint.dto.geo.*;
import com.civicsense.complaint.entity.geo.AdministrativeWard;
import com.civicsense.complaint.entity.geo.AdministrativeZone;
import com.civicsense.complaint.entity.geo.BoundaryProvenance;
import com.civicsense.complaint.repository.AdministrativeWardRepository;
import com.civicsense.complaint.repository.AdministrativeZoneRepository;
import com.civicsense.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for administrative geography management (Zones and Wards).
 *
 * <p>Enforces:
 * <ul>
 *   <li>Boundary integrity and range validation</li>
 *   <li>Scope-based code and ward number uniqueness per municipal authority</li>
 *   <li>Protection against silent overwrite or downgrade of verified delimitation boundaries</li>
 *   <li>Optional parent zone support for municipalities without intermediate zones</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdministrativeGeographyService {

    private static final String DEFAULT_MUNICIPALITY = "Municipal Corporation of Delhi (MCD)";
    private static final String DEFAULT_REGION = "Delhi NCR";

    private final AdministrativeZoneRepository zoneRepository;
    private final AdministrativeWardRepository wardRepository;

    // =========================================================================
    // Zone Operations
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ZoneResponse> getZones(String municipality, Boolean activeOnly) {
        List<AdministrativeZone> zones;
        boolean filterActive = Boolean.TRUE.equals(activeOnly);

        if (municipality != null && !municipality.isBlank()) {
            zones = filterActive
                    ? zoneRepository.findByMunicipalityAndActiveTrue(municipality.trim())
                    : zoneRepository.findByMunicipality(municipality.trim());
        } else if (filterActive) {
            zones = zoneRepository.findByActiveTrue();
        } else {
            zones = zoneRepository.findAll();
        }

        return zones.stream().map(this::mapZoneToResponse).toList();
    }

    @Transactional(readOnly = true)
    public ZoneResponse getZoneById(UUID id) {
        AdministrativeZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative zone not found with ID: " + id));
        return mapZoneToResponse(zone);
    }

    @Transactional
    public ZoneResponse createZone(CreateZoneRequest request) {
        String municipality = (request.municipality() != null && !request.municipality().isBlank())
                ? request.municipality().trim()
                : DEFAULT_MUNICIPALITY;
        String region = (request.region() != null && !request.region().isBlank())
                ? request.region().trim()
                : DEFAULT_REGION;
        String code = request.code().trim();

        if (zoneRepository.existsByMunicipalityAndCode(municipality, code)) {
            throw new IllegalArgumentException(
                    "Zone with code '" + code + "' already exists in municipality '" + municipality + "'"
            );
        }

        AdministrativeZone zone = new AdministrativeZone(request.name().trim(), code, municipality, region);
        zone.setDataSource(request.dataSource());
        zone.setActive(true);

        AdministrativeZone saved = zoneRepository.save(zone);
        log.info("Created administrative zone '{}' ({}) in municipality '{}'", saved.getName(), saved.getCode(), saved.getMunicipality());
        return mapZoneToResponse(saved);
    }

    @Transactional
    public ZoneResponse updateZone(UUID id, UpdateZoneRequest request) {
        AdministrativeZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative zone not found with ID: " + id));

        String targetMunicipality = (request.municipality() != null && !request.municipality().isBlank())
                ? request.municipality().trim()
                : zone.getMunicipality();

        if (request.code() != null && !request.code().isBlank()) {
            String newCode = request.code().trim();
            if (!newCode.equalsIgnoreCase(zone.getCode()) || !targetMunicipality.equalsIgnoreCase(zone.getMunicipality())) {
                if (zoneRepository.existsByMunicipalityAndCode(targetMunicipality, newCode)) {
                    throw new IllegalArgumentException(
                            "Zone with code '" + newCode + "' already exists in municipality '" + targetMunicipality + "'"
                    );
                }
            }
            zone.setCode(newCode);
        }

        if (request.name() != null && !request.name().isBlank()) {
            zone.setName(request.name().trim());
        }
        if (request.municipality() != null && !request.municipality().isBlank()) {
            zone.setMunicipality(request.municipality().trim());
        }
        if (request.region() != null && !request.region().isBlank()) {
            zone.setRegion(request.region().trim());
        }
        if (request.dataSource() != null) {
            zone.setDataSource(request.dataSource().trim());
        }
        if (request.active() != null) {
            zone.setActive(request.active());
        }

        AdministrativeZone saved = zoneRepository.save(zone);
        log.info("Updated administrative zone '{}' ({})", saved.getName(), saved.getCode());
        return mapZoneToResponse(saved);
    }

    // =========================================================================
    // Ward Operations
    // =========================================================================

    @Transactional(readOnly = true)
    public List<WardResponse> getWards(String municipality, UUID zoneId, Boolean activeOnly) {
        List<AdministrativeWard> wards;
        boolean filterActive = Boolean.TRUE.equals(activeOnly);

        if (zoneId != null) {
            wards = filterActive
                    ? wardRepository.findByZoneIdAndActiveTrue(zoneId)
                    : wardRepository.findByZoneId(zoneId);
        } else if (municipality != null && !municipality.isBlank()) {
            wards = filterActive
                    ? wardRepository.findByMunicipalityAndActiveTrue(municipality.trim())
                    : wardRepository.findByMunicipality(municipality.trim());
        } else if (filterActive) {
            wards = wardRepository.findByActiveTrue();
        } else {
            wards = wardRepository.findAll();
        }

        return wards.stream().map(this::mapWardToResponse).toList();
    }

    @Transactional(readOnly = true)
    public WardResponse getWardById(UUID id) {
        AdministrativeWard ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative ward not found with ID: " + id));
        return mapWardToResponse(ward);
    }

    @Transactional
    public WardResponse createWard(CreateWardRequest request) {
        AdministrativeZone parentZone = null;
        String municipality;

        if (request.zoneId() != null) {
            parentZone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent zone not found with ID: " + request.zoneId()));
            if (!parentZone.isActive()) {
                throw new IllegalArgumentException("Cannot attach ward to an inactive parent zone: " + parentZone.getName());
            }

            if (request.municipality() != null && !request.municipality().isBlank()
                    && !request.municipality().trim().equalsIgnoreCase(parentZone.getMunicipality())) {
                throw new IllegalArgumentException(
                        "Ward municipality '" + request.municipality() + "' must match parent zone municipality '" + parentZone.getMunicipality() + "'"
                );
            }
            municipality = parentZone.getMunicipality();
        } else {
            municipality = (request.municipality() != null && !request.municipality().isBlank())
                    ? request.municipality().trim()
                    : DEFAULT_MUNICIPALITY;
        }

        String wardNumber = request.wardNumber().trim();
        if (wardRepository.existsByMunicipalityAndWardNumber(municipality, wardNumber)) {
            throw new IllegalArgumentException(
                    "Ward with number '" + wardNumber + "' already exists in municipality '" + municipality + "'"
            );
        }

        BoundaryProvenance provenance = request.boundaryProvenance();
        if (provenance == null) {
            provenance = (request.minLatitude() != null)
                    ? BoundaryProvenance.PROVISIONAL_BOUNDS
                    : BoundaryProvenance.NO_BOUNDARY;
        }

        validateBoundary(
                provenance,
                request.minLatitude(), request.maxLatitude(),
                request.minLongitude(), request.maxLongitude(),
                request.centerLatitude(), request.centerLongitude()
        );

        AdministrativeWard ward = new AdministrativeWard(
                parentZone,
                request.name().trim(),
                wardNumber,
                municipality,
                provenance,
                request.minLatitude(),
                request.maxLatitude(),
                request.minLongitude(),
                request.maxLongitude(),
                request.centerLatitude(),
                request.centerLongitude()
        );
        ward.setDataSource(request.dataSource());
        ward.setActive(true);

        AdministrativeWard saved = wardRepository.save(ward);
        log.info("Created administrative ward '{}' ({}) in municipality '{}' [Provenance: {}]",
                saved.getName(), saved.getWardNumber(), saved.getMunicipality(), saved.getBoundaryProvenance());
        return mapWardToResponse(saved);
    }

    @Transactional
    public WardResponse updateWard(UUID id, UpdateWardRequest request) {
        AdministrativeWard ward = wardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative ward not found with ID: " + id));

        // 1. Parent zone update & municipality consistency
        AdministrativeZone targetZone = ward.getZone();
        String targetMunicipality = ward.getMunicipality();

        if (request.zoneId() != null) {
            if (targetZone == null || !targetZone.getId().equals(request.zoneId())) {
                targetZone = zoneRepository.findById(request.zoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parent zone not found with ID: " + request.zoneId()));
                if (!targetZone.isActive()) {
                    throw new IllegalArgumentException("Cannot attach ward to inactive zone: " + targetZone.getName());
                }
                targetMunicipality = targetZone.getMunicipality();
            }
        } else if (request.municipality() != null && !request.municipality().isBlank()) {
            targetMunicipality = request.municipality().trim();
            if (targetZone != null && !targetZone.getMunicipality().equalsIgnoreCase(targetMunicipality)) {
                throw new IllegalArgumentException(
                        "Ward municipality cannot diverge from parent zone municipality: " + targetZone.getMunicipality()
                );
            }
        }

        // 2. Ward number uniqueness check
        if (request.wardNumber() != null && !request.wardNumber().isBlank()) {
            String newNumber = request.wardNumber().trim();
            if (!newNumber.equalsIgnoreCase(ward.getWardNumber()) || !targetMunicipality.equalsIgnoreCase(ward.getMunicipality())) {
                if (wardRepository.existsByMunicipalityAndWardNumber(targetMunicipality, newNumber)) {
                    throw new IllegalArgumentException(
                            "Ward with number '" + newNumber + "' already exists in municipality '" + targetMunicipality + "'"
                    );
                }
            }
            ward.setWardNumber(newNumber);
        }

        // 3. Boundary provenance and coordinates update
        BoundaryProvenance targetProvenance = request.boundaryProvenance() != null
                ? request.boundaryProvenance()
                : ward.getBoundaryProvenance();

        Double targetMinLat = request.minLatitude() != null ? request.minLatitude() : ward.getMinLatitude();
        Double targetMaxLat = request.maxLatitude() != null ? request.maxLatitude() : ward.getMaxLatitude();
        Double targetMinLon = request.minLongitude() != null ? request.minLongitude() : ward.getMinLongitude();
        Double targetMaxLon = request.maxLongitude() != null ? request.maxLongitude() : ward.getMaxLongitude();
        Double targetCenterLat = request.centerLatitude() != null ? request.centerLatitude() : ward.getCenterLatitude();
        Double targetCenterLon = request.centerLongitude() != null ? request.centerLongitude() : ward.getCenterLongitude();

        // Protection: Prevent silent overwrite or downgrade of VERIFIED_BOUNDARY
        if (ward.getBoundaryProvenance() == BoundaryProvenance.VERIFIED_BOUNDARY) {
            boolean coordinatesAltered = !Objects.equals(ward.getMinLatitude(), targetMinLat)
                    || !Objects.equals(ward.getMaxLatitude(), targetMaxLat)
                    || !Objects.equals(ward.getMinLongitude(), targetMinLon)
                    || !Objects.equals(ward.getMaxLongitude(), targetMaxLon);

            boolean provenanceDowngraded = targetProvenance != BoundaryProvenance.VERIFIED_BOUNDARY;

            if ((coordinatesAltered || provenanceDowngraded) && !Boolean.TRUE.equals(request.forceBoundaryOverwrite())) {
                throw new IllegalStateException(
                        "Ward currently has a VERIFIED_BOUNDARY. Overwriting or downgrading verified municipal delimitation boundaries requires explicit forceBoundaryOverwrite confirmation."
                );
            }
        }

        validateBoundary(targetProvenance, targetMinLat, targetMaxLat, targetMinLon, targetMaxLon, targetCenterLat, targetCenterLon);

        ward.setZone(targetZone);
        ward.setMunicipality(targetMunicipality);
        ward.setBoundaryProvenance(targetProvenance);
        ward.setMinLatitude(targetMinLat);
        ward.setMaxLatitude(targetMaxLat);
        ward.setMinLongitude(targetMinLon);
        ward.setMaxLongitude(targetMaxLon);
        ward.setCenterLatitude(targetCenterLat);
        ward.setCenterLongitude(targetCenterLon);

        if (request.name() != null && !request.name().isBlank()) {
            ward.setName(request.name().trim());
        }
        if (request.dataSource() != null) {
            ward.setDataSource(request.dataSource().trim());
        }
        if (request.active() != null) {
            ward.setActive(request.active());
        }

        AdministrativeWard saved = wardRepository.save(ward);
        log.info("Updated administrative ward '{}' ({})", saved.getName(), saved.getWardNumber());
        return mapWardToResponse(saved);
    }

    // =========================================================================
    // Boundary Validation
    // =========================================================================

    private void validateBoundary(
            BoundaryProvenance provenance,
            Double minLat, Double maxLat,
            Double minLon, Double maxLon,
            Double centerLat, Double centerLon
    ) {
        if (provenance == BoundaryProvenance.NO_BOUNDARY) {
            if (minLat != null || maxLat != null || minLon != null || maxLon != null) {
                throw new IllegalArgumentException(
                        "Wards with provenance NO_BOUNDARY must not have bounding box coordinates. Fabricated coordinates are forbidden."
                );
            }
            return;
        }

        // Must provide non-null coordinates for VERIFIED_BOUNDARY or PROVISIONAL_BOUNDS
        if (minLat == null || maxLat == null || minLon == null || maxLon == null) {
            throw new IllegalArgumentException(
                    "Bounding box coordinates (min/max latitude and longitude) are required for provenance: " + provenance
            );
        }

        if (minLat < -90.0 || minLat > 90.0 || maxLat < -90.0 || maxLat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
        }
        if (minLon < -180.0 || minLon > 180.0 || maxLon < -180.0 || maxLon > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLatitude (" + minLat + ") cannot be greater than maxLatitude (" + maxLat + ")");
        }
        if (minLon > maxLon) {
            throw new IllegalArgumentException("minLongitude (" + minLon + ") cannot be greater than maxLongitude (" + maxLon + ")");
        }

        if (centerLat != null) {
            if (centerLat < -90.0 || centerLat > 90.0) {
                throw new IllegalArgumentException("centerLatitude must be between -90.0 and 90.0");
            }
            if (centerLat < minLat || centerLat > maxLat) {
                throw new IllegalArgumentException("centerLatitude (" + centerLat + ") must be within [minLatitude, maxLatitude]");
            }
        }
        if (centerLon != null) {
            if (centerLon < -180.0 || centerLon > 180.0) {
                throw new IllegalArgumentException("centerLongitude must be between -180.0 and 180.0");
            }
            if (centerLon < minLon || centerLon > maxLon) {
                throw new IllegalArgumentException("centerLongitude (" + centerLon + ") must be within [minLongitude, maxLongitude]");
            }
        }
    }

    // =========================================================================
    // DTO Mappers
    // =========================================================================

    private ZoneResponse mapZoneToResponse(AdministrativeZone zone) {
        return new ZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getCode(),
                zone.getMunicipality(),
                zone.getRegion(),
                zone.getDataSource(),
                zone.isActive(),
                zone.getWards() != null ? zone.getWards().size() : 0,
                zone.getCreatedAt(),
                zone.getUpdatedAt()
        );
    }

    private WardResponse mapWardToResponse(AdministrativeWard ward) {
        return new WardResponse(
                ward.getId(),
                ward.getZone() != null ? ward.getZone().getId() : null,
                ward.getZone() != null ? ward.getZone().getName() : null,
                ward.getName(),
                ward.getWardNumber(),
                ward.getMunicipality(),
                ward.getBoundaryProvenance(),
                ward.getDataSource(),
                ward.isActive(),
                ward.getMinLatitude(),
                ward.getMaxLatitude(),
                ward.getMinLongitude(),
                ward.getMaxLongitude(),
                ward.getCenterLatitude(),
                ward.getCenterLongitude(),
                ward.getAssignedOfficerId(),
                ward.getCreatedAt(),
                ward.getUpdatedAt()
        );
    }
}

