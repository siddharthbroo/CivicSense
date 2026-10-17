package com.civicsense.complaint.entity.geo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an Administrative Ward.
 *
 * <p>May belong to a parent AdministrativeZone (e.g. Hauz Khas in South Zone under MCD),
 * or directly to a Municipality when intermediate administrative zones do not exist.
 *
 * <p>Contains optional bounding box coordinates (min/max latitude and longitude)
 * and an optional centroid for geographic resolution of confirmed complaint map pins.
 *
 * <p>Boundary provenance explicitly tracks whether the ward boundaries are verified municipal
 * delimitation data, provisional bounding boxes for development/testing, or an official record
 * without spatial data yet (NO_BOUNDARY).
 */
@Entity
@Table(name = "administrative_wards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ward_municipality_number", columnNames = {"municipality", "ward_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeWard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Optional parent zone: some municipal bodies have no intermediate zone layer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = true)
    private AdministrativeZone zone;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "ward_number", nullable = false, length = 50)
    private String wardNumber; // e.g. "142-S", "055-C"

    @Column(nullable = false, length = 150)
    private String municipality = "Municipal Corporation of Delhi (MCD)";

    @Enumerated(EnumType.STRING)
    @Column(name = "boundary_provenance", nullable = false, length = 30)
    private BoundaryProvenance boundaryProvenance = BoundaryProvenance.PROVISIONAL_BOUNDS;

    @Column(name = "data_source", length = 255)
    private String dataSource;

    @Column(nullable = false)
    private boolean active = true;

    // Geographic bounding box for map-pin resolution (nullable when NO_BOUNDARY)
    @Column(name = "min_latitude", nullable = true)
    private Double minLatitude;

    @Column(name = "max_latitude", nullable = true)
    private Double maxLatitude;

    @Column(name = "min_longitude", nullable = true)
    private Double minLongitude;

    @Column(name = "max_longitude", nullable = true)
    private Double maxLongitude;

    // Centroid of the ward (nullable)
    @Column(name = "center_latitude", nullable = true)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = true)
    private Double centerLongitude;

    // Placeholder for future officer assignment (intentionally unassigned for now)
    @Column(name = "assigned_officer_id")
    private UUID assignedOfficerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (boundaryProvenance == null) {
            boundaryProvenance = hasBoundary() ? BoundaryProvenance.PROVISIONAL_BOUNDS : BoundaryProvenance.NO_BOUNDARY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Backward-compatible constructor for existing seed data and tests.
     */
    public AdministrativeWard(
            AdministrativeZone zone,
            String name,
            String wardNumber,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon,
            Double centerLat,
            Double centerLon
    ) {
        this.zone = zone;
        this.name = name;
        this.wardNumber = wardNumber;
        this.municipality = (zone != null && zone.getMunicipality() != null)
                ? zone.getMunicipality()
                : "Municipal Corporation of Delhi (MCD)";
        this.boundaryProvenance = BoundaryProvenance.PROVISIONAL_BOUNDS;
        this.minLatitude = minLat;
        this.maxLatitude = maxLat;
        this.minLongitude = minLon;
        this.maxLongitude = maxLon;
        this.centerLatitude = centerLat;
        this.centerLongitude = centerLon;
        this.active = true;
    }

    /**
     * Constructor for wards with or without parent zone and with explicit provenance.
     */
    public AdministrativeWard(
            AdministrativeZone zone,
            String name,
            String wardNumber,
            String municipality,
            BoundaryProvenance boundaryProvenance,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon,
            Double centerLat,
            Double centerLon
    ) {
        this.zone = zone;
        this.name = name;
        this.wardNumber = wardNumber;
        this.municipality = (municipality != null && !municipality.isBlank())
                ? municipality
                : (zone != null && zone.getMunicipality() != null ? zone.getMunicipality() : "Municipal Corporation of Delhi (MCD)");
        this.boundaryProvenance = (boundaryProvenance != null)
                ? boundaryProvenance
                : (minLat != null ? BoundaryProvenance.PROVISIONAL_BOUNDS : BoundaryProvenance.NO_BOUNDARY);
        this.minLatitude = minLat;
        this.maxLatitude = maxLat;
        this.minLongitude = minLon;
        this.maxLongitude = maxLon;
        this.centerLatitude = centerLat;
        this.centerLongitude = centerLon;
        this.active = true;
    }

    public boolean hasBoundary() {
        return minLatitude != null && maxLatitude != null
                && minLongitude != null && maxLongitude != null;
    }

    /**
     * Checks if given latitude and longitude falls within this ward's bounding box.
     */
    public boolean contains(Double lat, Double lon) {
        if (lat == null || lon == null || !hasBoundary()) return false;
        return lat >= minLatitude && lat <= maxLatitude
                && lon >= minLongitude && lon <= maxLongitude;
    }
}