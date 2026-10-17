package com.civicsense.complaint.service.geo;

import com.civicsense.complaint.entity.geo.AdministrativeWard;
import com.civicsense.complaint.entity.geo.AdministrativeZone;
import com.civicsense.complaint.entity.geo.BoundaryProvenance;
import com.civicsense.complaint.repository.AdministrativeWardRepository;
import com.civicsense.complaint.repository.AdministrativeZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing geographic administrative hierarchy (Zone -> Ward)
 * and resolving confirmed map pins to the appropriate administrative unit.
 */
@Service
@RequiredArgsConstructor
public class GeographicHierarchyService {

    private final AdministrativeZoneRepository zoneRepository;
    private final AdministrativeWardRepository wardRepository;

    /**
     * Resolves a confirmed complaint map pin (latitude, longitude) to an Administrative Ward.
     * Uses bounding box lookup first; if multiple overlapping wards are found,
     * prioritizes verified boundaries over provisional bounds, then picks closest centroid.
     *
     * @param latitude  Confirmed incident latitude
     * @param longitude Confirmed incident longitude
     * @return Optional containing the resolved AdministrativeWard, or empty if out of bounds.
     */
    @Transactional(readOnly = true)
    public Optional<AdministrativeWard> resolveWard(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        List<AdministrativeWard> candidateWards = wardRepository.findWardsContainingPoint(latitude, longitude);

        if (candidateWards.isEmpty()) {
            return Optional.empty();
        }

        if (candidateWards.size() == 1) {
            return Optional.of(candidateWards.get(0));
        }

        // Multiple matches: prioritize VERIFIED_BOUNDARY over PROVISIONAL_BOUNDS, then closest centroid
        return candidateWards.stream()
                .min(Comparator
                        .comparing((AdministrativeWard w) -> w.getBoundaryProvenance() == BoundaryProvenance.VERIFIED_BOUNDARY ? 0 : 1)
                        .thenComparingDouble(w -> distanceSquared(
                                latitude, longitude,
                                w.getCenterLatitude() != null ? w.getCenterLatitude() : (w.getMinLatitude() + w.getMaxLatitude()) / 2.0,
                                w.getCenterLongitude() != null ? w.getCenterLongitude() : (w.getMinLongitude() + w.getMaxLongitude()) / 2.0
                        )));
    }

    private double distanceSquared(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return (dLat * dLat) + (dLon * dLon);
    }
}