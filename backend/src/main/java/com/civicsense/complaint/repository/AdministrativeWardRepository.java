package com.civicsense.complaint.repository;

import com.civicsense.complaint.entity.geo.AdministrativeWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdministrativeWardRepository extends JpaRepository<AdministrativeWard, UUID> {

    Optional<AdministrativeWard> findByWardNumber(String wardNumber);

    Optional<AdministrativeWard> findByMunicipalityAndWardNumber(String municipality, String wardNumber);

    List<AdministrativeWard> findByZoneId(UUID zoneId);

    List<AdministrativeWard> findByZoneIdAndActiveTrue(UUID zoneId);

    List<AdministrativeWard> findByMunicipality(String municipality);

    List<AdministrativeWard> findByActiveTrue();

    List<AdministrativeWard> findByMunicipalityAndActiveTrue(String municipality);

    boolean existsByMunicipalityAndWardNumber(String municipality, String wardNumber);

    /**
     * Finds active wards with defined boundaries whose bounding box contains the specified coordinates.
     * Uses LEFT JOIN FETCH so wards without a parent zone are also resolved properly.
     */
    @Query("""
        SELECT w FROM AdministrativeWard w
        LEFT JOIN FETCH w.zone
        WHERE w.active = true
          AND w.minLatitude IS NOT NULL
          AND :lat >= w.minLatitude AND :lat <= w.maxLatitude
          AND :lon >= w.minLongitude AND :lon <= w.maxLongitude
    """)
    List<AdministrativeWard> findWardsContainingPoint(
            @Param("lat") Double latitude,
            @Param("lon") Double longitude
    );
}