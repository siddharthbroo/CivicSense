package com.civicsense.complaint.entity.geo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an Administrative Zone in a municipality (e.g. South Zone, Central Zone, Rohini Zone under MCD).
 * Hierarchy: Municipality -> AdministrativeZone (optional) -> AdministrativeWard -> Future Officer
 */
@Entity
@Table(name = "administrative_zones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_zone_municipality_code", columnNames = {"municipality", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdministrativeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String code; // e.g. "DL-SOUTH", "DL-CENTRAL"

    @Column(nullable = false, length = 150)
    private String municipality = "Municipal Corporation of Delhi (MCD)";

    @Column(length = 100)
    private String region = "Delhi NCR";

    @Column(name = "data_source", length = 255)
    private String dataSource;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdministrativeWard> wards = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AdministrativeZone(String name, String code) {
        this.name = name;
        this.code = code;
        this.municipality = "Municipal Corporation of Delhi (MCD)";
        this.region = "Delhi NCR";
        this.active = true;
    }

    public AdministrativeZone(String name, String code, String municipality, String region) {
        this.name = name;
        this.code = code;
        this.municipality = (municipality != null && !municipality.isBlank()) ? municipality : "Municipal Corporation of Delhi (MCD)";
        this.region = (region != null && !region.isBlank()) ? region : "Delhi NCR";
        this.active = true;
    }
}